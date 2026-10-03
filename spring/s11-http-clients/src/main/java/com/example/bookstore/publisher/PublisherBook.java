package com.example.bookstore.publisher;

import java.time.LocalDate;

/**
 * A book as the publishers' catalog describes it:
 *
 * <pre>{@code
 * { "isbn": "9791115881394", "title": "Effective Java", "publisher": "Insight",
 *   "publishedDate": "2018-10-18", "listPrice": 36000 }
 * }</pre>
 *
 * @param listPrice the price that the publisher recommends, in won
 */
public record PublisherBook(String isbn, String title, String publisher, LocalDate publishedDate, long listPrice) {
}
