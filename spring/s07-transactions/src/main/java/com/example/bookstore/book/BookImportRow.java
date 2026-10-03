package com.example.bookstore.book;

import java.time.LocalDate;

/**
 * One row of the publishers' nightly catalog file: a new book, and how many copies of it arrived.
 *
 * @param isbn     13 digits, without hyphens
 * @param category the name of an existing category, such as {@code Programming}
 * @param price    in won
 * @param stock    how many copies arrived; 0 or more
 */
public record BookImportRow(String isbn, String title, String author, String category, long price,
                            LocalDate publishedDate, int stock) {
}
