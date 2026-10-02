package com.example.bookstore.order;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * The result of a checkout.
 *
 * @param lines       one line per book, in ascending book id order
 * @param subtotal    the sum of the lines' amounts, in won
 * @param shippingFee the shipping fee in won
 * @param orderedAt   when the checkout happened, as told by the application's {@link java.time.Clock}
 */
public record Receipt(List<OrderLine> lines, long subtotal, long shippingFee, Instant orderedAt) {

    public Receipt {
        lines = List.copyOf(lines); // a receipt never changes, even if the caller's list does
        Objects.requireNonNull(orderedAt, "orderedAt");
    }

    /** What the customer pays: {@code subtotal + shippingFee}, in won. */
    public long total() {
        return subtotal + shippingFee;
    }
}
