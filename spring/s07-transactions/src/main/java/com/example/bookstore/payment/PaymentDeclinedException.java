package com.example.bookstore.payment;

import lombok.Getter;

/**
 * Thrown when a payment doesn't go through, for example because the wallet balance is too low.
 *
 * <p>This is a <em>checked</em> exception: the team that designed the payment API wanted every caller to handle a
 * declined payment explicitly. Most exceptions in this codebase are unchecked {@code BusinessException}s instead.
 */
@Getter
public class PaymentDeclinedException extends Exception {

    /** The order that couldn't be paid. */
    private final long orderId;

    public PaymentDeclinedException(long orderId, String message) {
        super(message);
        this.orderId = orderId;
    }
}
