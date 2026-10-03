package com.example.bookstore.inventory;

import org.springframework.util.Assert;

/**
 * How many copies of one book are in stock. It shares its id with the book. From S06 on it is a JPA entity with
 * methods that take copies out of stock and put them back.
 *
 * @param bookId   the id of the book
 * @param quantity copies in stock; never negative
 */
public record Inventory(long bookId, int quantity) {

    public Inventory {
        Assert.isTrue(quantity >= 0, () -> "quantity must not be negative: " + quantity);
    }

    public boolean isInStock() {
        return quantity > 0;
    }
}
