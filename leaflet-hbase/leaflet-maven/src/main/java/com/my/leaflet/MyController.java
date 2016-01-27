package com.my.leaflet;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

@RestController
public class MyController {

    @Autowired
    @Qualifier("phoenixDataSource")
    private DataSource phoenixDataSource;

    @RequestMapping("/medallion/{medallion}")
    public String medallion(@PathVariable("medallion") String medallion, @Value("${sql.medallion}") String sql) {
        System.out.println(String.format("handing a query for medallion=%s", medallion));
        JdbcTemplate jdbc = new JdbcTemplate(phoenixDataSource);
        String json = jdbc.queryForObject(sql, new Object[] { medallion + ":" }, String.class);
        return json;
    }

    @RequestMapping("/medallion/{medallion}/pickupTime/{pickupTime}")
    public String trip(@PathVariable("medallion") String medallion, @PathVariable("pickupTime") String pickupTime, @Value("${sql.trip}") String sql) {
        System.out.println(String.format("handing a query for medallion=%s, pickupTime=%s", medallion, pickupTime));
        String rowkey = String.format("%s:%s", medallion, pickupTime);
        JdbcTemplate jdbc = new JdbcTemplate(phoenixDataSource);
        String xml = jdbc.queryForObject(sql, new Object[] { rowkey }, String.class);
        JsonElement json = new JsonPrimitive(xml);
        return json.toString();
    }

}
