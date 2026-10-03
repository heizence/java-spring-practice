package com.example.bookstore.cart;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.example.bookstore.book.BookCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Mutant: a ShoppingCart bean without @Scope("prototype"), so it's a singleton that every member shares. */
@Component
@RequiredArgsConstructor
public class ShoppingCart {

    private final BookCatalog bookCatalog;
    private final Map<Long, Integer> quantitiesByBookId = new LinkedHashMap<>();

    public void add(long bookId, int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("The quantity must be at least 1, but was %d.".formatted(quantity));
        }
        if (bookCatalog.findById(bookId).isEmpty()) {
            throw new IllegalArgumentException("Book %d doesn't exist.".formatted(bookId));
        }
        quantitiesByBookId.merge(bookId, quantity, Integer::sum);
    }

    public List<CartLine> lines() {
        return quantitiesByBookId.entrySet().stream()
                .map(entry -> new CartLine(entry.getKey(), entry.getValue()))
                .toList();
    }

    public long totalAmount() {
        return quantitiesByBookId.entrySet().stream()
                .mapToLong(entry -> bookCatalog.findById(entry.getKey()).orElseThrow().price() * entry.getValue())
                .sum();
    }

    public boolean isEmpty() {
        return quantitiesByBookId.isEmpty();
    }
}
