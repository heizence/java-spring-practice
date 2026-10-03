package com.example.bookstore.book;

import java.util.List;

import com.example.bookstore.inventory.InventoryService;
import com.example.bookstore.review.ReviewResponse;
import com.example.bookstore.review.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * What a book's page shows besides the book's details: its numbers and its best reviews. The page is the store's
 * most visited one, so the services behind it cache what they read.
 */
@Service
@RequiredArgsConstructor
public class BookPageService {

    static final int TOP_REVIEWS = 3;

    private final ReviewService reviewService;
    private final InventoryService inventoryService;

    /** How many reviews the book has, and how many copies are in stock. */
    public BookStats stats(long bookId) {
        return new BookStats(reviewService.countReviews(bookId), inventoryService.copiesInStock(bookId));
    }

    /**
     * The book's best reviews, at most {@value #TOP_REVIEWS}: the highest ratings first, the newest first among equal
     * ratings ({@link ReviewResponse#BEST_FIRST}). The page's reviews tab still lists all reviews newest first.
     */
    public List<ReviewResponse> topReviews(long bookId) {
        // The list comes from the cache and belongs to every caller: build a new list instead of sorting it. (Its
        // subList would be a view of the cached list, too.)
        return reviewService.findReviews(bookId).stream()
                .sorted(ReviewResponse.BEST_FIRST)
                .limit(TOP_REVIEWS)
                .toList();
    }
}
