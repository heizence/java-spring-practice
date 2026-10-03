package com.example.bookstore.payment;

/**
 * What {@link PaymentService#pay} did.
 *
 * @param payment the payment
 * @param created {@code true} if this request created it, {@code false} if an earlier request with the same
 *                idempotency key did
 */
public record PaymentResult(PaymentResponse payment, boolean created) {
}
