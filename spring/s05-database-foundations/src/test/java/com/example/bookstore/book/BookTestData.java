package com.example.bookstore.book;

import java.time.LocalDate;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.example.bookstore.support.TestClock;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

/**
 * Inserts rows for tests with plain SQL, independently of the code under test, so that a bug in that code can't
 * hide behind a bug in the test data. The tests run in a transaction that is rolled back, so the rows disappear
 * after each test.
 */
final class BookTestData {

    /** The seeded books' ISBNs start with 9791, so test ISBNs (9790…) never collide with them. */
    private static final AtomicLong NEXT_ISBN = new AtomicLong(9_790_000_000_000L);

    private BookTestData() {
    }

    /** A fresh ISBN that no other book has. */
    static String newIsbn() {
        return Long.toString(NEXT_ISBN.incrementAndGet());
    }

    /** Inserts a category and returns its generated id. */
    static long insertCategory(JdbcClient jdbc, String name) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.sql("INSERT INTO categories (name) VALUES (:name)").param("name", name).update(keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }

    /** Inserts a book with a fresh ISBN and returns its generated id. */
    static long insertBook(JdbcClient jdbc, long categoryId, String title, String author, long price,
                           LocalDate publishedDate) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO books (isbn, title, author, category_id, price, published_date, created_at, updated_at)
                        VALUES (:isbn, :title, :author, :categoryId, :price, :publishedDate, :now, :now)""")
                .param("isbn", newIsbn())
                .param("title", title)
                .param("author", author)
                .param("categoryId", categoryId)
                .param("price", price)
                .param("publishedDate", publishedDate)
                .param("now", TestClock.DEFAULT_INSTANT)
                .update(keyHolder);
        return Objects.requireNonNull(keyHolder.getKey()).longValue();
    }
}
