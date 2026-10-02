package com.example.bookstore.discount;

/**
 * A rule for how much of an order's amount the customer doesn't pay. Each rule is a bean of its own, named after
 * what it's for, such as {@code goldDiscount}.
 */
public interface DiscountPolicy {

    /**
     * The discount in won for an order of {@code amount} won: at least 0, at most {@code amount}.
     *
     * @param amount the order's amount in won, at least 0
     */
    long discountFor(long amount);
}
