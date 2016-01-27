package com.my.leaflet.prepare;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

@Configuration
public class PhoenixTable {

    public static ApplicationContext appContext;
    public static JdbcTemplate jdbc;
    public static List<String> files;
    public static Map<String, Map<String, Map<String, Trip>>> carSummary = new HashMap<String, Map<String, Map<String, Trip>>>();

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PhoenixTable.class);
        app.setWebEnvironment(false);
        appContext = app.run(args);
        init(args);
        try {
            prepareSampleGpsFiles();
            createTripTable();
            insertTrips();
            calcCarSummary();
            insertCarSummary();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("done");
    }

    /**
     * Init JDBC connection to HBase via Phoenix
     */
    public static void init(String[] args) {
        String driverClassName = appContext.getEnvironment().getProperty("phoenix.jdbc.driver");
        String url = appContext.getEnvironment().getProperty("phoenix.jdbc.url");
        BasicDataSource dataSource = new BasicDataSource();
        dataSource.setDriverClassName(driverClassName);
        dataSource.setUrl(url);
        jdbc = new JdbcTemplate(dataSource);
    }

    /**
     * this function put GPS data in a list. Later they will be randomly selected as car GPS data.
     */
    public static void prepareSampleGpsFiles() throws Exception {
        files = new ArrayList<String>();
        final String PATH = "data/WorkingTracks/kml/";
        String filenames = "track2.kml track4.kml track5.kml 2015-09-10_15_16_32.kml 2015-09-10_17_59_14.kml 2015-09-11_09_18_33.kml 2015-09-12_13_53_50.kml 2015-09-12_19_57_45.kml 2015-09-14_09_48_18.kml 2015-09-09_15_46_52.kml";
        String[] splits = filenames.split(" ");
        for (String filename : splits) {
            BufferedReader br = new BufferedReader(new FileReader(PATH + filename));
            StringBuilder fileString = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                fileString.append(line);
            }
            br.close();
            files.add(fileString.toString());
        }
    }

    public static void createTripTable() {
        System.out.println("creating HBase trip table via Phoenix");
        jdbc.execute(appContext.getEnvironment().getProperty("sql.drop_trip_table"));
        jdbc.execute(appContext.getEnvironment().getProperty("sql.create_trip_table"));
    }

    /**
     * This function reads from sample trip data file and inserts into trip table.
     * For each trip, it also insert a random trip GPS data.
     */
    public static void insertTrips() throws Exception {
        System.out.println("inserting sample trip data into HBase via Phoenix");
        String sql = appContext.getEnvironment().getProperty("sql.insert_trip_table");
        int[] argTypes = new int[16];
        for (int index = 0; index < 16; index++) {
            argTypes[index] = Types.VARCHAR;
        }
        String filename = appContext.getEnvironment().getProperty("car.csv.relative");
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        int count = 0;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            String[] splits = line.split(",");
            if (splits.length != 14) continue;
            String[] args = new String[16];
            args[0] = splits[0] + ":" + splits[5];
            for (int index = 0; index < 14; index++) {
                args[index + 1] = splits[index];
            }
            int random = Double.valueOf(Math.random() * files.size()).intValue();
            args[15] = files.get(random);
            jdbc.update(sql, args, argTypes);
            System.out.println(++count);
        }
        br.close();
    }

    /**
     * This function organizes trip records by car then by day in a tree structure.
     */
    public static void calcCarSummary() throws Exception {
        String filename = appContext.getEnvironment().getProperty("car.csv.relative");
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = br.readLine()) != null) {
            if (line.trim().isEmpty()) continue;
            Trip trip = new Trip(line);
            if (!carSummary.containsKey(trip.getMedallion())) {
                carSummary.put(trip.getMedallion(), new TreeMap<String, Map<String, Trip>>());
            }
            if (!carSummary.get(trip.getMedallion()).containsKey(trip.getTripDate())) {
                carSummary.get(trip.getMedallion()).put(trip.getTripDate(), new TreeMap<String, Trip>());
            }
            carSummary.get(trip.getMedallion()).get(trip.getTripDate()).put(trip.getTripDatetime(), trip);
        }
        br.close();
    }

    public static JsonObject wrapJson(Trip trip) {
        JsonObject tripJson = new JsonObject();
        tripJson.add("trip:pickup_datetime", new JsonPrimitive(trip.get("trip:pickup_datetime")));
        tripJson.add("trip:dropoff_datetime", new JsonPrimitive(trip.get("trip:dropoff_datetime")));
        tripJson.add("trip:passenger_count", new JsonPrimitive(Integer.valueOf(trip.get("trip:passenger_count"))));
        tripJson.add("trip:trip_time_in_secs", new JsonPrimitive(Integer.valueOf(trip.get("trip:trip_time_in_secs"))));
        tripJson.add("trip:trip_distance", new JsonPrimitive(Double.valueOf(trip.get("trip:trip_distance"))));
        tripJson.add("trip:pickup_location", new JsonArray());
        tripJson.get("trip:pickup_location").getAsJsonArray()
                .add(new JsonPrimitive(Double.valueOf(trip.get("trip:pickup_latitude"))));
        tripJson.get("trip:pickup_location").getAsJsonArray()
                .add(new JsonPrimitive(Double.valueOf(trip.get("trip:pickup_longitude"))));
        tripJson.add("trip:dropoff_location", new JsonArray());
        tripJson.get("trip:dropoff_location").getAsJsonArray()
                .add(new JsonPrimitive(Double.valueOf(trip.get("trip:dropoff_latitude"))));
        tripJson.get("trip:dropoff_location").getAsJsonArray()
                .add(new JsonPrimitive(Double.valueOf(trip.get("trip:dropoff_longitude"))));
        return tripJson;
    }

    /**
     * this function insert one record for each car containing all trip information
     * for the car in Json format.
     */
    public static void insertCarSummary() throws Exception {
        System.out.println("insert car summary data");
        String sql = appContext.getEnvironment().getProperty("sql.insert_trip_aggregate");
        int[] argTypes = new int[] { Types.VARCHAR, Types.VARCHAR };
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (String medallion : carSummary.keySet()) {
            JsonObject days = new JsonObject();
            for (String date : carSummary.get(medallion).keySet()) {
                days.add(date, new JsonArray());
                for (String datetime : carSummary.get(medallion).get(date).keySet()) {
                    Trip trip = carSummary.get(medallion).get(date).get(datetime);
                    days.get(date).getAsJsonArray().add(wrapJson(trip));
                }
            }

            String rowkey = medallion + ":";
            String total = gson.toJson(days);
            jdbc.update(sql, new Object[] { rowkey, total }, argTypes);
        }
    }

}
