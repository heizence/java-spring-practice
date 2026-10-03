package com.example.bookstore.common;

import java.util.List;

/**
 * The names of the application's caches. Annotations refer to these constants instead of repeating the names:
 * {@code @Cacheable(CacheNames.BOOKS)}. {@link CacheConfiguration} creates exactly the caches in {@link #ALL}, so a
 * new cache needs a constant here and an entry in that list.
 */
public final class CacheNames {

    /** {@code BookResponse}s, by book id. */
    public static final String BOOKS = "books";

    /** The list of all categories, under one key. */
    public static final String CATEGORIES = "categories";

    /** The reviews of a book, newest first, by book id. */
    public static final String REVIEWS = "reviews";

    // One cache per kind of value. A key is only the method's arguments (here, the book id), so two methods that
    // share a cache share their entries: the review count and the stock of book 1 were both stored under the key 1.

    /** How many reviews a book has, by book id. */
    public static final String REVIEW_COUNTS = "reviewCounts";

    /** How many copies of a book are in stock, by book id. */
    public static final String STOCK = "stock";

    /** Every cache of the application. */
    public static final List<String> ALL = List.of(BOOKS, CATEGORIES, REVIEWS, REVIEW_COUNTS, STOCK);

    private CacheNames() {
    }
}
