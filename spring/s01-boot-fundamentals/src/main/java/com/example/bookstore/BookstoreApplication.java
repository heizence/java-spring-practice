package com.example.bookstore;

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
public class BookstoreApplication {

    public static void main(String[] args) {
        // TODO: S01-01
        throw new UnsupportedOperationException("TODO: S01-01");
    }
}
