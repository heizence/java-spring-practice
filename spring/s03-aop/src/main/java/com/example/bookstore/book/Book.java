package com.example.bookstore.book;

import java.util.Objects;

/**
 * A book in the catalog, as the API shows it. S03 has no database, so books are plain records kept in memory by
 * {@link BookService}. From S06 on, {@code Book} is a JPA entity and the API shows a {@code BookResponse}.
 *
 * @param category the category's name
 * @param price    the current price in won
 */
public record Book(long id, String isbn, String title, String author, String category, long price) {

    public Book {
        Objects.requireNonNull(isbn, "isbn");
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(author, "author");
        Objects.requireNonNull(category, "category");
    }
}
