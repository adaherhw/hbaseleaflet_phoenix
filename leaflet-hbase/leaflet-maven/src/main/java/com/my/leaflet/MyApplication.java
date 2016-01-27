package com.my.leaflet;

import javax.sql.DataSource;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootApplication
public class MyApplication {

        public static void main(String[] args) {
                new SpringApplication(MyApplication.class).run(args);
        }

        @Bean
        public DataSource phoenixDataSource(@Value("${phoenix.jdbc.driver}") String driver, @Value("${phoenix.jdbc.url}")  String url) {
                BasicDataSource dataSource = new BasicDataSource();
                dataSource.setDriverClassName(driver);
                dataSource.setUrl(url);
                System.out.println("+++++++++++++++do a query to make sure the data source is initialized correctly+++++++++++++++++++");
                JdbcTemplate jdbc = new JdbcTemplate(dataSource);
                jdbc.queryForObject("select table_name from system.catalog limit 1", String.class);
                System.out.println("++++++++++++++++successfully loaded datasource+++++++++++++++++++");
                return dataSource;
        }

}
