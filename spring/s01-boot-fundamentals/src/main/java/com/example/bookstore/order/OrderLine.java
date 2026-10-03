package com.example.bookstore.order;

import java.util.Objects;

/**
 * One book in a checkout: which book, at what price, and how many copies.
 *
 * @param bookId    the book's id
 * @param title     the book's title, so that a receipt can show it without asking the catalog again
 * @param unitPrice the price of one copy in won, when the checkout happened
 * @param quantity  the number of copies, at least 1
 */
public record OrderLine(long bookId, String title, long unitPrice, int quantity) {

    /** @throws IllegalArgumentException if {@code quantity} is below 1. The message names the book and the quantity. */
    public OrderLine {
        Objects.requireNonNull(title, "title");
        if (quantity < 1) {
            throw new IllegalArgumentException(
                    "The quantity of book %d must be at least 1, but was %d.".formatted(bookId, quantity));
        }
    }

    /** What this line costs: {@code unitPrice × quantity}, in won. */
    public long amount() {
        return unitPrice * quantity;
    }
}
