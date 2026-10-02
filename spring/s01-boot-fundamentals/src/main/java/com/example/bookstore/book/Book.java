package com.example.bookstore.book;

/**
 * A book in the catalog.
 *
 * <p>This module has no database yet, so a book is a plain record. From S06 on, {@code Book} is a JPA entity with the
 * same fields, and its category is an entity of its own.
 *
 * @param id       the book's id, from 1
 * @param isbn     the 13-digit ISBN, without hyphens
 * @param category the name of the book's category, such as {@code Programming}
 * @param price    the current price in won
 */
public record Book(long id, String isbn, String title, String author, String category, long price) {
}
