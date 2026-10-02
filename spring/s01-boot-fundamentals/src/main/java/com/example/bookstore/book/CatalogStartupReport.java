package com.example.bookstore.book;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;

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
public class CatalogStartupReport implements ApplicationRunner {

    @Override
    public void run(ApplicationArguments args) {
        // TODO: S01-01
        throw new UnsupportedOperationException("TODO: S01-01");
    }
}
