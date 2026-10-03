package com.example.bookstore.cart;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.bookstore.book.BookCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/**
 * One member's shopping cart: which books, and how many copies of each.
 *
 * <p>A cart holds the state of one member, so every member needs a cart of their own: this bean must not be a
 * singleton. One member uses a cart at a time, so it isn't thread-safe.
 */
// Prototype scope: the container creates a new ShoppingCart, with its BookCatalog injected, every time someone asks
// for one. Unlike a singleton, Spring doesn't keep track of a prototype after handing it out, so it never calls a
// prototype's @PreDestroy method.
@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequiredArgsConstructor
public class ShoppingCart {

    private final BookCatalog bookCatalog;

    /** Keeps the books in the order they were first added. */
    private final Map<Long, Integer> quantitiesByBookId = new LinkedHashMap<>();

    /**
     * Adds copies of a book. Adding a book that is already in the cart increases its quantity.
     *
     * @throws IllegalArgumentException if the book doesn't exist or {@code quantity} is below 1. The cart doesn't
     *                                  change then.
     */
    public void add(long bookId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("The quantity must be at least 1, but was %d.".formatted(quantity));
        }
        if (bookCatalog.findById(bookId).isEmpty()) {
            throw new IllegalArgumentException("Book %d doesn't exist.".formatted(bookId));
        }
        quantitiesByBookId.merge(bookId, quantity, Integer::sum);
    }

    /** The cart's lines, in the order the books were first added, as an unmodifiable list. */
    public List<CartLine> lines() {
        return quantitiesByBookId.entrySet().stream()
                .map(entry -> new CartLine(entry.getKey(), entry.getValue()))
                .toList();
    }

    /** What the books in the cart cost at the catalog's current prices, in won. */
    public long totalAmount() {
        return quantitiesByBookId.entrySet().stream()
                .mapToLong(entry -> bookCatalog.findById(entry.getKey()).orElseThrow().price() * entry.getValue())
                .sum();
    }

    public boolean isEmpty() {
        return quantitiesByBookId.isEmpty();
    }
}
