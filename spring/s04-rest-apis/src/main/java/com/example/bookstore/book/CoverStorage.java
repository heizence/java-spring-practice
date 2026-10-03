package com.example.bookstore.book;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

/**
 * Stores the cover images of books in memory, one per book: {@code coverStorage.findById(bookId)}. A real
 * application keeps files out of the database, in object storage such as Amazon S3, and stores only their address.
 *
 * <p>The methods are {@code synchronized}: Tomcat serves each request on its own thread, so several threads use this
 * one bean at the same time.
 */
@Repository
public class CoverStorage {

    private final Map<Long, Cover> covers = new HashMap<>();

    public synchronized Optional<Cover> findById(long bookId) {
        return Optional.ofNullable(covers.get(bookId));
    }

    /** Stores the cover of a book, replacing the one it had before. */
    public synchronized void save(long bookId, Cover cover) {
        covers.put(bookId, cover);
    }

    /** Removes the cover of a book; does nothing if it has none. */
    public synchronized void deleteById(long bookId) {
        covers.remove(bookId);
    }

    public synchronized void deleteAll() {
        covers.clear();
    }
}
