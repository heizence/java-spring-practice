package com.example.bookstore.order;

import java.util.function.Supplier;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * The order metrics, recorded in Micrometer's {@link MeterRegistry}:
 *
 * <ul>
 *   <li>Counter {@value #PLACED}: how many orders were placed. It has no tags, and it exists from the start, with
 *       the value 0, so that a dashboard shows 0 instead of "no data" before the first order.</li>
 *   <li>Timer {@value #PLACEMENT}: how long placing an order takes, with the tag {@code outcome}: {@code placed}
 *       when the order was placed, {@code rejected} when placing it threw an exception, such as out of stock.</li>
 * </ul>
 *
 * <p>Tag values come from a small, fixed set. A member id, an order id or a book title would create a new time
 * series for every value, and the metrics system would have to keep every one of them.
 */
@Component
@RequiredArgsConstructor
public class OrderMetrics {

    public static final String PLACED = "bookstore.orders.placed";
    public static final String PLACEMENT = "bookstore.orders.placement";

    private final MeterRegistry registry;

    /**
     * Places an order through {@code placement} and records the metrics above: counts the order if it was placed,
     * and times the call with its outcome. An exception from {@code placement} reaches the caller unchanged.
     *
     * @return what {@code placement} returned
     */
    public OrderResponse recordPlacement(Supplier<OrderResponse> placement) {
        // TODO: S14-02
        throw new UnsupportedOperationException("TODO: S14-02");
    }
}
