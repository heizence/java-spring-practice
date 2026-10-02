package com.example.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/** Mutant: configuration and auto-configuration, but no component scanning. */
@SpringBootConfiguration
@EnableAutoConfiguration
public class BookstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreApplication.class, args);
    }
}
