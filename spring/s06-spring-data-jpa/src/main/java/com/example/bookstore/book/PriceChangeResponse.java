package com.example.bookstore.book;

import java.util.List;

/**
 * The result of a price change, so the purchasing team can check it.
 *
 * @param category     the category's name
 * @param changedBooks how many rows the {@code UPDATE} statement changed
 * @param books        every book of the category with its old and new price, in id order
 */
public record PriceChangeResponse(Long categoryId, String category, int percent, int changedBooks,
                                  List<BookPriceChange> books) {

    /** One book's price before and after the change, in won. */
    public record BookPriceChange(Long bookId, String title, long oldPrice, long newPrice) {
    }
}
