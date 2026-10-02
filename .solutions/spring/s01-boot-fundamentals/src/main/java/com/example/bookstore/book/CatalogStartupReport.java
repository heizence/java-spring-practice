package com.example.bookstore.book;

import java.util.List;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Logs a summary of the catalog when the application has started, so that whoever starts the bookstore can see at a
 * glance that the catalog is in place.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>It's a bean that component scanning finds ({@code @Component}). Because it's an {@link ApplicationRunner},
 *       Spring Boot calls {@link #run} once, after the application has started.</li>
 *   <li>It gets the {@link BookCatalog} through its constructor: a {@code final} field and Lombok's
 *       {@code @RequiredArgsConstructor}.</li>
 *   <li>{@link #run} logs one line at INFO level with Lombok's {@code @Slf4j} logger:
 *       {@code The catalog holds 60 books in 6 categories.} Both numbers come from the catalog: how many books it
 *       holds, and how many different category names they have.</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogStartupReport implements ApplicationRunner {

    private final BookCatalog bookCatalog;

    // An ApplicationRunner runs after the context has started, so every bean is ready. A @PostConstruct method would
    // run in the middle of startup instead, before Spring Boot logs "Started BookstoreApplication".
    @Override
    public void run(ApplicationArguments args) {
        List<Book> books = bookCatalog.findAll();
        long categories = books.stream().map(Book::category).distinct().count();
        // Placeholders ({}) instead of string concatenation: the message is only built if INFO is enabled.
        log.info("The catalog holds {} books in {} categories.", books.size(), categories);
    }
}
