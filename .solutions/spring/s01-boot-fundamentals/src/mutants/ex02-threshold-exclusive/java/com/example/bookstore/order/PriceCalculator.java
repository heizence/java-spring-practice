package com.example.bookstore.order;

import java.util.List;

import org.springframework.stereotype.Component;

/** Mutant: ships free only above 20,000 won, so an order of exactly 20,000 won still pays shipping. */
@Component
public class PriceCalculator {

    public static final long SHIPPING_FEE = 3_000;

    public static final long FREE_SHIPPING_FROM = 20_000;

    public long subtotal(List<OrderLine> lines) {
        return lines.stream().mapToLong(OrderLine::amount).sum();
    }

    public long shippingFee(long subtotal) {
        if (subtotal < 0) {
            throw new IllegalArgumentException("The subtotal must not be negative, but was %d.".formatted(subtotal));
        }
        return subtotal > FREE_SHIPPING_FROM ? 0 : SHIPPING_FEE;
    }
}
