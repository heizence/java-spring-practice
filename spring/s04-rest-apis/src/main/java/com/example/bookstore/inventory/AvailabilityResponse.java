package com.example.bookstore.inventory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;

/**
 * Whether and when a book can be shipped, as {@code GET /api/books/{id}/availability} shows it.
 *
 * @param bookId            the id of the book
 * @param status            how many copies are left, roughly
 * @param quantity          the copies in stock
 * @param orderBy           order before this moment, and the book ships the same day; {@code null} when the book is
 *                          out of stock. The warehouse computes it in Korean time, so it carries the zone
 *                          {@code Asia/Seoul}.
 * @param estimatedDelivery the day the book arrives if ordered now; {@code null} when the book is out of stock
 * @param checkedAt         when this answer was computed
 */
public record AvailabilityResponse(long bookId, Availability status, int quantity, ZonedDateTime orderBy,
                                   LocalDate estimatedDelivery, Instant checkedAt) {
}
