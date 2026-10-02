package com.example.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The online bookstore that every Part 2 module works on. {@code spring/BOOKSTORE.md} describes its domain, data and
 * conventions.
 *
 * <p>Keep this class free of other annotations such as {@code @ConfigurationPropertiesScan}: tests that start parts of
 * the application begin from this class, and every feature switched on here would have to start in them too. Put
 * such settings in their own {@code @Configuration} class instead.
 */
@SpringBootApplication
public class BookstoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookstoreApplication.class, args);
    }
}
