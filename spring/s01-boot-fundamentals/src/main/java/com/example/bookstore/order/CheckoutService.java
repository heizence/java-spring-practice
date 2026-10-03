package com.example.bookstore.order;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookCatalog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Checks out an order: looks each book up in the catalog, prices the order and stamps it with the current time.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>It's a bean ({@code @Service}). It gets a {@link BookCatalog}, a {@link PriceCalculator} and a {@link Clock}
 *       through its only constructor, into {@code final} fields. Let Lombok's {@code @RequiredArgsConstructor} write
 *       the constructor, and don't use {@code @Autowired}.</li>
 *   <li>{@link #checkout} creates one {@link OrderLine} per book, with the book's title and current price from the
 *       catalog, in ascending book id order.</li>
 *   <li>The receipt's subtotal and shipping fee come from the {@link PriceCalculator}. Its {@code orderedAt} is
 *       {@code clock.instant()}. Never call {@code Instant.now()}: tests replace the clock with one they control.</li>
 *   <li>An empty order, an unknown book id or a quantity below 1 throws {@link IllegalArgumentException}, with a
 *       message that names the problem, such as the unknown book id. A {@code null} map throws
 *       {@link NullPointerException}.</li>
 * </ul>
 */
// @RequiredArgsConstructor generates CheckoutService(BookCatalog, PriceCalculator, Clock), from the final fields in
// their order. It's the only constructor, so Spring uses it without @Autowired.
@Service
@RequiredArgsConstructor
public class CheckoutService {

    // final: the dependencies are set once, by the constructor, and a test can pass its own (a fixed Clock, say).
    private final BookCatalog bookCatalog;
    private final PriceCalculator priceCalculator;
    private final Clock clock;

    /**
     * Checks out the given books.
     *
     * @param quantitiesByBookId how many copies of each book, by book id
     */
    public Receipt checkout(Map<Long, Integer> quantitiesByBookId) {
        Objects.requireNonNull(quantitiesByBookId, "quantitiesByBookId");
        if (quantitiesByBookId.isEmpty()) {
            throw new IllegalArgumentException("An order needs at least one book.");
        }
        List<OrderLine> lines = quantitiesByBookId.entrySet().stream()
                .sorted(Map.Entry.comparingByKey()) // a map has no reliable order; the receipt does
                .map(entry -> lineFor(entry.getKey(), entry.getValue()))
                .toList();
        long subtotal = priceCalculator.subtotal(lines);
        return new Receipt(lines, subtotal, priceCalculator.shippingFee(subtotal), clock.instant());
    }

    private OrderLine lineFor(long bookId, int quantity) {
        Book book = bookCatalog.findById(bookId)
                .orElseThrow(() -> new IllegalArgumentException("Book %d doesn't exist.".formatted(bookId)));
        return new OrderLine(book.id(), book.title(), book.price(), quantity); // OrderLine checks the quantity
    }
}
