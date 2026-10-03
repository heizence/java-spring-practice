package com.example.bookstore.book;

import java.time.LocalDate;

/**
 * A book as the API shows it.
 *
 * @param category the category's name
 * @param price    the current price in won
 */
public record BookResponse(Long id, String isbn, String title, String author, String category, long price,
                           LocalDate publishedDate) {

    /** Reads the book's category, so call it while the book's persistence context is open. */
    static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getCategory().getName(), book.getPrice(), book.getPublishedDate());
    }
}
