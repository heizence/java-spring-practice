package com.example.bookstore.order;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Mutant: stamps the receipt with Instant.now() instead of the injected Clock. */
@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final BookCatalog bookCatalog;
    private final PriceCalculator priceCalculator;
    private final Clock clock;

    public Receipt checkout(Map<Long, Integer> quantitiesByBookId) {
        Objects.requireNonNull(quantitiesByBookId, "quantitiesByBookId");
        if (quantitiesByBookId.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one book.");
        }
        List<OrderLine> lines = quantitiesByBookId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> lineFor(entry.getKey(), entry.getValue()))
                .toList();
        long subtotal = priceCalculator.subtotal(lines);
        return new Receipt(lines, subtotal, priceCalculator.shippingFee(subtotal), Instant.now());
    }

    private OrderLine lineFor(long bookId, int quantity) {
        Book book = bookCatalog.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book %d doesn't exist.".formatted(bookId)));
        return new OrderLine(book.id(), book.title(), book.price(), quantity);
    }
}
