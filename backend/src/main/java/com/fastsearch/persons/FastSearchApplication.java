package com.fastsearch.persons;

import com.fastsearch.persons.config.FastSearchProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** Point d'entrée du backend FastSearch Persons. */
@SpringBootApplication
@EnableConfigurationProperties(FastSearchProperties.class)
public class FastSearchApplication {

    public static void main(String[] args) {
        SpringApplication.run(FastSearchApplication.class, args);
    }
}
