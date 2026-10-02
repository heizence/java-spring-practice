package com.example.bookstore.book;

import java.time.Clock;
import java.util.List;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Reads and writes books with plain SQL through Spring's {@link JdbcClient}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Every value that comes from a caller is a bound parameter ({@code :id}), never part of the SQL text.</li>
 *   <li>Books come back as {@link Book} records, with the name of their category.</li>
 *   <li>The times this class writes come from the {@link Clock} bean, never from {@code Instant.now()}.</li>
 *   <li>Database errors reach the caller as the {@link DataAccessException}s that {@link JdbcClient} throws.</li>
 * </ul>
 */
@Repository
@RequiredArgsConstructor
public class BookJdbcRepository {

    /** The largest page that {@link #findByCategory} returns. */
    public static final int MAX_PAGE_SIZE = 100;

    private final JdbcClient jdbc;
    private final Clock clock;

    /**
     * The book with this id, with its category's name.
     *
     * @return the book, or empty if no book has this id
     */
    public Optional<Book> findById(long id) {
        // TODO: S05-02
        throw new UnsupportedOperationException("TODO: S05-02");
    }

    /**
     * The books whose title contains {@code keyword}, ignoring letter case, sorted by title and then by id.
     *
     * @return the matching books, or an empty list if none match
     * @throws IllegalArgumentException if {@code keyword} is {@code null} or blank
     */
    public List<Book> searchByTitle(String keyword) {
        // TODO: S05-02
        throw new UnsupportedOperationException("TODO: S05-02");
    }

    /**
     * One page of the books in a category, sorted by id. Page 0 holds the first {@code size} books, page 1 the next
     * {@code size} books, and so on.
     *
     * @param page the page number, from 0
     * @param size the number of books per page, from 1 to {@value #MAX_PAGE_SIZE}
     * @return the books on that page: an empty list for a page after the last one, or for a category without books
     * @throws IllegalArgumentException if {@code page} is negative or {@code size} is outside 1 to
     *                                  {@value #MAX_PAGE_SIZE}
     */
    public List<Book> findByCategory(long categoryId, int page, int size) {
        // TODO: S05-02
        throw new UnsupportedOperationException("TODO: S05-02");
    }

    /**
     * Adds a book to the catalog. Its {@code created_at} and {@code updated_at} are both the clock's current time.
     *
     * @return the id that MySQL generated for the new row
     * @throws IllegalArgumentException        if {@code book} is {@code null}
     * @throws DuplicateKeyException           if a book with the same ISBN exists; nothing is inserted
     * @throws DataIntegrityViolationException if the category doesn't exist; nothing is inserted
     */
    public long insert(NewBook book) {
        // TODO: S05-02
        throw new UnsupportedOperationException("TODO: S05-02");
    }

    /**
     * Changes the price of one book, and sets its {@code updated_at} to the clock's current time.
     *
     * @return the number of books changed: 1, or 0 if no book has this id
     * @throws IllegalArgumentException if {@code newPrice} is below 1; nothing changes
     */
    public int updatePrice(long id, long newPrice) {
        // TODO: S05-02
        throw new UnsupportedOperationException("TODO: S05-02");
    }
}
