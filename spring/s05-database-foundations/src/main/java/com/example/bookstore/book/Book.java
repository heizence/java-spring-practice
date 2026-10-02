package com.example.bookstore.book;

import java.time.LocalDate;

/**
 * A book as the API shows it: one row of the {@code books} table, together with its category's name.
 *
 * <p>This module reads books with plain SQL, so it copies each row into a new, immutable record. From S06 on,
 * Hibernate maps the table to an entity instead.
 *
 * @param category the name of the book's category, such as {@code Programming}
 * @param price    the current price in won
 */
public record Book(long id, String isbn, String title, String author, String category, long price,
                   LocalDate publishedDate) {
}
