package com.example.bookstore.book;

import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * A book as the API shows it.
 *
 * @param category the category's name
 * @param price    the current price in won
 * @param version  the book's version. It travels in the {@code ETag} response header (S04-07), not in the JSON body:
 *                 {@code @JsonIgnore} leaves it out of the JSON.
 */
public record BookResponse(Long id, String isbn, String title, String author, String category, long price,
                           LocalDate publishedDate, @JsonIgnore long version) {

    static BookResponse from(Book book) {
        return new BookResponse(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                book.getCategory().name(), book.getPrice(), book.getPublishedDate(), book.getVersion());
    }
}
