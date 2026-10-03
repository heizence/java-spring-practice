package com.example.bookstore.book;

/**
 * The numbers on a book's page.
 *
 * @param reviewCount   how many reviews the book has
 * @param copiesInStock how many copies are in stock
 */
public record BookStats(long reviewCount, long copiesInStock) {
}
