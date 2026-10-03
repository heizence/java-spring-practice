package com.example.bookstore.book;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import org.springframework.util.Assert;

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

    /**
     * The columns of {@link Book}, in every query. {@code query(Book.class)} maps each column to the record component
     * of the same name, after turning {@code snake_case} into {@code camelCase}: {@code published_date} becomes
     * {@code publishedDate}. The category's column is called {@code name}, so the alias makes it {@code category}.
     */
    private static final String SELECT_BOOKS = """
            SELECT b.id, b.isbn, b.title, b.author, c.name AS category, b.price, b.published_date
            FROM books b
            JOIN categories c ON c.id = b.category_id
            """;

    private final JdbcClient jdbc;
    private final Clock clock;

    /**
     * The book with this id, with its category's name.
     *
     * @return the book, or empty if no book has this id
     */
    public Optional<Book> findById(long id) {
        return jdbc.sql(SELECT_BOOKS + "WHERE b.id = :id")
                .param("id", id)
                .query(Book.class)
                .optional(); // empty for no row; throws if a query that should find one row finds several
    }

    /**
     * The books whose title contains {@code keyword}, ignoring letter case, sorted by title and then by id.
     *
     * @return the matching books, or an empty list if none match
     * @throws IllegalArgumentException if {@code keyword} is {@code null} or blank
     */
    public List<Book> searchByTitle(String keyword) {
        Assert.hasText(keyword, "keyword must not be blank");
        // The wildcards are part of the bound value, so the keyword never becomes SQL. MySQL's default collation
        // (utf8mb4_0900_ai_ci) compares without regard to letter case, so LOWER() isn't needed.
        return jdbc.sql(SELECT_BOOKS + "WHERE b.title LIKE :pattern ORDER BY b.title, b.id")
                .param("pattern", "%" + keyword + "%")
                .query(Book.class)
                .list();
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
        Assert.isTrue(page >= 0, () -> "page must not be negative: " + page);
        Assert.isTrue(size >= 1 && size <= MAX_PAGE_SIZE,
                () -> "size must be between 1 and %d: %d".formatted(MAX_PAGE_SIZE, size));
        // ORDER BY is what makes paging work: without it, MySQL may return the rows in any order, and the same book
        // could show up on two pages.
        return jdbc.sql(SELECT_BOOKS + "WHERE b.category_id = :categoryId ORDER BY b.id LIMIT :size OFFSET :offset")
                .param("categoryId", categoryId)
                .param("size", size)
                .param("offset", page)
                .query(Book.class)
                .list();
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
        Assert.notNull(book, "book must not be null");
        Instant now = clock.instant();
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO books (isbn, title, author, category_id, price, published_date, created_at, updated_at)
                        VALUES (:isbn, :title, :author, :categoryId, :price, :publishedDate, :now, :now)""")
                .param("isbn", book.isbn())
                .param("title", book.title())
                .param("author", book.author())
                .param("categoryId", book.categoryId())
                .param("price", book.price())
                .param("publishedDate", book.publishedDate())
                .param("now", now) // one named parameter, used twice
                .update(keyHolder);
        // update() itself returns the number of inserted rows (1). The new id comes back through the key holder;
        // MySQL Connector/J reports it as a BigInteger, so convert it instead of casting.
        return Objects.requireNonNull(keyHolder.getKey(), "MySQL returned no generated id").longValue();
    }

    /**
     * Changes the price of one book, and sets its {@code updated_at} to the clock's current time.
     *
     * @return the number of books changed: 1, or 0 if no book has this id
     * @throws IllegalArgumentException if {@code newPrice} is below 1; nothing changes
     */
    public int updatePrice(long id, long newPrice) {
        Assert.isTrue(newPrice > 0, () -> "newPrice must be positive: " + newPrice);
        return jdbc.sql("UPDATE books SET price = :price, updated_at = :now WHERE id = :id")
                .param("price", newPrice)
                .param("now", clock.instant())
                .param("id", id)
                .update();
    }
}
