package com.example.bookstore.review;

import java.time.Instant;

/**
 * A review as the book page shows it.
 *
 * @param reviewer  the name of the member who wrote the review
 * @param rating    1 to 5
 * @param createdAt when the review was written
 */
public record ReviewResponse(Long id, String reviewer, int rating, String content, Instant createdAt) {
}
