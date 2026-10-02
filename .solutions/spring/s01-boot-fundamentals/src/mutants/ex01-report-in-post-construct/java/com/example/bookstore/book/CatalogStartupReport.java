package com.example.bookstore.book;

import java.util.List;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Mutant: logs the summary in a @PostConstruct method, in the middle of startup, instead of as an ApplicationRunner. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CatalogStartupReport {

    private final BookCatalog bookCatalog;

    @PostConstruct
    void report() {
        List<Book> books = bookCatalog.findAll();
        long categories = books.stream().map(Book::category).distinct().count();
        log.info("The catalog holds {} books in {} categories.", books.size(), categories);
    }
}
