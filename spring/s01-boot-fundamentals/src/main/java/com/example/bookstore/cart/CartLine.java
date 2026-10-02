package com.example.bookstore.cart;

/**
 * One book in a shopping cart.
 *
 * @param bookId   the book's id
 * @param quantity the number of copies, at least 1
 */
public record CartLine(long bookId, int quantity) {

    /** @throws IllegalArgumentException if {@code quantity} is below 1 */
    public CartLine {
        if (quantity < 1) {
            throw new IllegalArgumentException("The quantity must be at least 1, but was %d.".formatted(quantity));
        }
    }
}
