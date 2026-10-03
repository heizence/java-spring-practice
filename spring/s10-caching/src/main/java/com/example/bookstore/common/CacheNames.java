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

    /** Numbers shown on a book's page, by book id. */
    public static final String BOOK_STATS = "bookStats";

    /** Every cache of the application. */
    public static final List<String> ALL = List.of(BOOKS, CATEGORIES, REVIEWS, BOOK_STATS);

    private CacheNames() {
    }
}
