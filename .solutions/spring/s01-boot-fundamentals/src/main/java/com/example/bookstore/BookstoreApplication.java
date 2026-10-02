package com.example.bookstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The online bookstore that every Part 2 module works on. {@code spring/BOOKSTORE.md} describes its domain, data and
 * conventions.
 *
 * <h2>Rules (S01-01)</h2>
 * <ul>
 *   <li>It's a Spring Boot application: {@code @SpringBootApplication} switches on configuration, auto-configuration
 *       and component scanning from this class's package, {@code com.example.bookstore}, downwards.</li>
 *   <li>{@link #main} starts it with {@code SpringApplication.run}, and passes the command-line arguments on, so that
 *       {@code --name=value} arguments become configuration properties.</li>
 * </ul>
 *
 * <p>Keep this class free of other annotations such as {@code @EnableScheduling}: tests that start parts of the
 * application begin from this class, and every feature switched on here would have to start in them too. Put such
 * settings in their own {@code @Configuration} class instead.
 */
// @SpringBootApplication = @SpringBootConfiguration (a @Configuration) + @EnableAutoConfiguration + @ComponentScan.
// The scan starts in this class's package, which is why the class sits in the root package, above every feature.
@SpringBootApplication
public class BookstoreApplication {

    public static void main(String[] args) {
        // Pass args on: SpringApplication turns "--spring.application.name=x" into a property with top precedence.
        SpringApplication.run(BookstoreApplication.class, args);
    }
}
