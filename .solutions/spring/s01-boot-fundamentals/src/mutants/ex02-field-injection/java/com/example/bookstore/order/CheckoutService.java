package com.example.bookstore.order;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** Mutant: field injection. It works at runtime, but the fields aren't final and a test can't pass its own clock. */
@Service
public class CheckoutService {

    @Autowired
    private BookCatalog bookCatalog;

    @Autowired
    private PriceCalculator priceCalculator;

    @Autowired
    private Clock clock;

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
        return new Receipt(lines, subtotal, priceCalculator.shippingFee(subtotal), clock.instant());
    }

    private OrderLine lineFor(long bookId, int quantity) {
        Book book = bookCatalog.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book %d doesn't exist.".formatted(bookId)));
        return new OrderLine(book.id(), book.title(), book.price(), quantity);
    }
}
