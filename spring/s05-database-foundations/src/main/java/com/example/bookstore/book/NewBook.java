package com.example.bookstore.book;

import java.time.LocalDate;

import org.springframework.util.Assert;

/**
 * A book to add to the catalog: everything {@link BookJdbcRepository#insert} needs. MySQL generates the id.
 *
 * <p>The constructor checks what the {@code books} table can't check by itself. The table checks the rest: a
 * duplicate ISBN breaks its unique key, and an unknown category breaks its foreign key.
 *
 * @param isbn       13 digits, without hyphens
 * @param categoryId the id of the book's category
 * @param price      the price in won, at least 1
 */
public record NewBook(String isbn, String title, String author, long categoryId, long price,
                      LocalDate publishedDate) {

    public NewBook {
        Assert.isTrue(isbn != null && isbn.matches("\\d{13}"), () -> "isbn must be 13 digits: " + isbn);
        Assert.hasText(title, "title must not be blank");
        Assert.hasText(author, "author must not be blank");
        Assert.isTrue(price > 0, () -> "price must be positive: " + price);
        Assert.notNull(publishedDate, "publishedDate must not be null");
    }
}
