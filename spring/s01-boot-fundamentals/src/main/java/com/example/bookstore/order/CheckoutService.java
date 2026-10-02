package com.example.bookstore.order;

import java.time.Clock;
import java.util.Map;

import com.example.bookstore.book.BookCatalog;

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
public class CheckoutService {

    /**
     * Checks out the given books.
     *
     * @param quantitiesByBookId how many copies of each book, by book id
     */
    public Receipt checkout(Map<Long, Integer> quantitiesByBookId) {
        // TODO: S01-02
        throw new UnsupportedOperationException("TODO: S01-02");
    }
}
