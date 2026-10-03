package com.example.bookstore.order;

import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Works out what an order costs. The pricing rules live here, in one place, so that every use case that prices an
 * order applies the same rules.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>It's a bean that component scanning finds ({@code @Component}). It has no state, so one instance can serve
 *       every request.</li>
 *   <li>{@link #subtotal} is the sum of the lines' {@link OrderLine#amount() amounts}. No lines: 0.</li>
 *   <li>{@link #shippingFee}: {@value #SHIPPING_FEE} won for a subtotal below {@value #FREE_SHIPPING_FROM} won, and
 *       0 from {@value #FREE_SHIPPING_FROM} won on. A negative subtotal throws {@link IllegalArgumentException}.</li>
 * </ul>
 */
@Component
public class PriceCalculator {

    /** The shipping fee in won for an order whose subtotal is below {@link #FREE_SHIPPING_FROM}. */
    public static final long SHIPPING_FEE = 3_000;

    /** Orders with a subtotal of at least this many won ship for free. */
    public static final long FREE_SHIPPING_FROM = 20_000;

    /** The sum of the lines' amounts, in won. */
    public long subtotal(List<OrderLine> lines) {
        return lines.stream().mapToLong(OrderLine::amount).sum();
    }

    /** The shipping fee in won for an order with this subtotal. */
    public long shippingFee(long subtotal) {
        if (subtotal < 0) {
            throw new IllegalArgumentException("The subtotal must not be negative, but was %d.".formatted(subtotal));
        }
        // ">=": an order of exactly 20,000 won ships free. S02-01 moves both numbers into the configuration.
        return subtotal >= FREE_SHIPPING_FROM ? 0 : SHIPPING_FEE;
    }
}
