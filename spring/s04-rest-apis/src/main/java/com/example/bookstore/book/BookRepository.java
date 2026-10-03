package com.example.bookstore.book;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import org.springframework.stereotype.Repository;

/**
 * Stores {@link Book}s in memory. It stands in for the {@code books} table, which arrives in S05, and offers the
 * methods of the Spring Data repository you'll use from S06 on.
 *
 * <p>It behaves like a database in the ways that matter for the API:
 * <ul>
 *   <li>It stores copies. A {@code Book} you read is yours: changing it changes nothing until you {@link #save} it,
 *       and two requests never share one {@code Book} object.</li>
 *   <li>{@link #save} increases the version of a changed book by one and rejects a book that someone else changed
 *       in the meantime, like JPA's {@code @Version} (S08). A save that changes nothing keeps the version.</li>
 *   <li>ISBNs are unique, like a unique key in the database.</li>
 * </ul>
 *
 * <p>The methods are {@code synchronized}: Tomcat serves each request on its own thread, so several threads use this
 * one bean at the same time.
 */
@Repository
public class BookRepository {

    private final TreeMap<Long, Book> books = new TreeMap<>();
    private long lastId;

    /** Every book, by id. */
    public synchronized List<Book> findAll() {
        return books.values().stream().map(Book::copy).toList();
    }

    public synchronized Optional<Book> findById(long id) {
        return Optional.ofNullable(books.get(id)).map(Book::copy);
    }

    public synchronized boolean existsById(long id) {
        return books.containsKey(id);
    }

    public synchronized boolean existsByIsbn(String isbn) {
        return books.values().stream().anyMatch(book -> book.getIsbn().equals(isbn));
    }

    /**
     * Stores a new book (one without an id) or a change to a stored one, and returns what is stored now: use the
     * returned book, which has the id and the version.
     *
     * @throws BusinessException {@link ErrorCode#DUPLICATE_ISBN} if another book has the new book's ISBN;
     *                           {@link ErrorCode#BOOK_NOT_FOUND} if the changed book was deleted meanwhile;
     *                           {@link ErrorCode#PRECONDITION_FAILED} if another request saved a change to the book
     *                           after this one read it
     */
    public synchronized Book save(Book book) {
        if (book.getId() == null) {
            if (existsByIsbn(book.getIsbn())) {
                throw new BusinessException(ErrorCode.DUPLICATE_ISBN,
                        "A book with ISBN %s already exists.".formatted(book.getIsbn()));
            }
            Book saved = book.withId(++lastId);
            books.put(saved.getId(), saved);
            return saved.copy();
        }
        Book stored = books.get(book.getId());
        if (stored == null) {
            throw new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Book %d was not found.".formatted(book.getId()));
        }
        if (stored.getVersion() != book.getVersion()) {
            throw new BusinessException(ErrorCode.PRECONDITION_FAILED,
                    "Book %d was changed by another request. Read it again and retry.".formatted(book.getId()));
        }
        if (stored.hasSameStateAs(book)) {
            return stored.copy();
        }
        Book saved = book.withNextVersion();
        books.put(saved.getId(), saved);
        return saved.copy();
    }

    /** Removes the book; does nothing if there is none with this id. */
    public synchronized void deleteById(long id) {
        books.remove(id);
    }

    public synchronized long count() {
        return books.size();
    }

    /** Removes every book and starts the ids at 1 again, like SQL's {@code TRUNCATE}. */
    public synchronized void deleteAll() {
        books.clear();
        lastId = 0;
    }
}
