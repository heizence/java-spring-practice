package com.example.bookstore.book;

import java.time.LocalDate;
import java.util.Objects;

import lombok.Getter;
import org.springframework.util.Assert;

/**
 * A book in the catalog.
 *
 * <p>In this module the catalog lives in memory ({@link BookRepository}); from S05 on it lives in MySQL, and from S06
 * on this class is a JPA entity. It already follows the entity rules of this codebase: no setters, a factory that
 * checks its arguments, and methods that change the state only in allowed ways. An invalid argument that request
 * validation should have stopped (a blank title, a price of 0) is a programming error and throws
 * {@link IllegalArgumentException}.
 */
@Getter
public class Book {

    /** Assigned by {@link BookRepository#save} when the book is first saved; {@code null} before that. */
    private Long id;

    /** The 13-digit ISBN, without hyphens. Unique, and never changes: a new edition gets a new ISBN. */
    private final String isbn;

    private String title;

    private String author;

    private Category category;

    /** The current price in won. */
    private long price;

    private LocalDate publishedDate;

    /**
     * Increases by one each time {@link BookRepository#save} stores a change. The API sends it as the book's
     * {@code ETag}, so that clients can update a book only if nobody changed it since they read it (S04-07).
     */
    private long version;

    private Book(Long id, String isbn, String title, String author, Category category, long price,
                 LocalDate publishedDate, long version) {
        Assert.isTrue(isbn != null && isbn.matches("\\d{13}"), () -> "isbn must be 13 digits: " + isbn);
        this.id = id;
        this.isbn = isbn;
        this.version = version;
        change(title, author, category, price, publishedDate);
    }

    /** A new book, not saved yet. Its version starts at 0. */
    public static Book create(String isbn, String title, String author, Category category, long price,
                              LocalDate publishedDate) {
        return new Book(null, isbn, title, author, category, price, publishedDate, 0);
    }

    /**
     * Replaces everything about the book that may change. The ISBN never changes. Nothing is stored until the book
     * is saved again.
     */
    public void update(String title, String author, Category category, long price, LocalDate publishedDate) {
        change(title, author, category, price, publishedDate);
    }

    private void change(String title, String author, Category category, long price, LocalDate publishedDate) {
        Assert.hasText(title, "title must not be blank");
        Assert.hasText(author, "author must not be blank");
        Assert.notNull(category, "category must not be null");
        Assert.notNull(category.id(), "category must be saved");
        Assert.isTrue(price > 0, () -> "price must be positive: " + price);
        Assert.notNull(publishedDate, "publishedDate must not be null");
        this.title = title.strip();
        this.author = author.strip();
        this.category = category;
        this.price = price;
        this.publishedDate = publishedDate;
    }

    // ---------------------------------------------------------------- for BookRepository only

    /** A copy, so that the repository's stored state changes only through {@link BookRepository#save}. */
    Book copy() {
        return new Book(id, isbn, title, author, category, price, publishedDate, version);
    }

    /** A copy of this unsaved book with the id that the repository assigned. */
    Book withId(long id) {
        return new Book(id, isbn, title, author, category, price, publishedDate, version);
    }

    /** A copy with the next version: what the repository stores when the book changed. */
    Book withNextVersion() {
        return new Book(id, isbn, title, author, category, price, publishedDate, version + 1);
    }

    /** Whether the two describe the same book in the same state, apart from the version. */
    boolean hasSameStateAs(Book other) {
        return Objects.equals(id, other.id) && isbn.equals(other.isbn) && title.equals(other.title)
                && author.equals(other.author) && category.equals(other.category) && price == other.price
                && publishedDate.equals(other.publishedDate);
    }
}
