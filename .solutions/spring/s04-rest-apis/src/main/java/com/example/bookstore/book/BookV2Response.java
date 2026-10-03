package com.example.bookstore.book;

import java.time.LocalDate;

/**
 * A book as version 2 of the API shows it ({@code API-Version: 2}). Two changes would break clients of version 1,
 * which is why they need a new version:
 *
 * <pre>{@code
 * {
 *   "id": 1,
 *   "isbn": "9791115881394",
 *   "title": "Effective Java",
 *   "author": "Joshua Bloch",
 *   "category": { "id": 1, "name": "Programming" },
 *   "price": { "amount": 36000, "currency": "KRW" },
 *   "publishedDate": "2018-10-18"
 * }
 * }</pre>
 *
 * <ul>
 *   <li>{@code category} is an object with the category's id and name, instead of the name alone, so that the app
 *       can link to the category.</li>
 *   <li>{@code price} is an amount with its currency, always {@code KRW} for now, so that the international shop
 *       can add others later.</li>
 * </ul>
 */
public record BookV2Response(long id, String isbn, String title, String author, CategoryRef category, Money price,
                             LocalDate publishedDate) {

    /** The currency of every price in this bookstore: Korean won. */
    static final String CURRENCY = "KRW";

    /** A category, as an object of its own. */
    public record CategoryRef(long id, String name) {
    }

    /** An amount of money: a whole number in the currency's smallest unit, which for won is the won itself. */
    public record Money(long amount, String currency) {
    }

    /** The version 2 representation of a book. */
    static BookV2Response from(Book book) {
        Category category = book.getCategory();
        return new BookV2Response(book.getId(), book.getIsbn(), book.getTitle(), book.getAuthor(),
                new CategoryRef(category.id(), category.name()), new Money(book.getPrice(), CURRENCY),
                book.getPublishedDate());
    }
}
