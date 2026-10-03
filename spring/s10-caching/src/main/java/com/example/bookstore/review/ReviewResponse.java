package com.example.bookstore.review;

import java.time.Instant;
import java.util.Comparator;

/**
 * A review as the API shows it.
 *
 * @param memberId the member who wrote it
 * @param rating   1 to 5
 */
public record ReviewResponse(Long id, Long memberId, int rating, String content, Instant createdAt) {

    /** The highest rating first; among equal ratings, the newest first. */
    public static final Comparator<ReviewResponse> BEST_FIRST = Comparator
            .comparingInt(ReviewResponse::rating).reversed()
            .thenComparing(ReviewResponse::createdAt, Comparator.reverseOrder())
            .thenComparing(ReviewResponse::id, Comparator.reverseOrder());

    /** Reads the review's member id, which doesn't load the member. */
    static ReviewResponse from(Review review) {
        return new ReviewResponse(review.getId(), review.getMember().getId(), review.getRating(),
                review.getContent(), review.getCreatedAt());
    }
}
