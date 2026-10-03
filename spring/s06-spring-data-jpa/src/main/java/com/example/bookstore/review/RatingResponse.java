package com.example.bookstore.review;

/**
 * How members rate a book.
 *
 * @param reviewCount   how many reviews the book has
 * @param averageRating the average of their ratings as the database computes it, or {@code null} if the book has
 *                      no reviews yet
 */
public record RatingResponse(Long bookId, long reviewCount, Double averageRating) {
}
