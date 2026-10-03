package com.example.bookstore.book;

/**
 * Numbers about the catalog for the admin dashboard: {@code GET /api/admin/books/stats}.
 *
 * @param books      how many books the catalog has
 * @param categories how many categories it has
 * @param outOfStock how many of its books have no copies in stock
 */
public record CatalogStatsResponse(long books, long categories, long outOfStock) {
}
