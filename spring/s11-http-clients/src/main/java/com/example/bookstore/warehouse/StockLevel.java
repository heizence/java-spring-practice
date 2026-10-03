package com.example.bookstore.warehouse;

/**
 * The warehouse's answer to {@code GET /v1/stock/{isbn}}: {@code { "isbn": "9791115881394", "available": 12 }}.
 *
 * @param available how many copies the warehouse can send right away
 */
public record StockLevel(String isbn, int available) {
}
