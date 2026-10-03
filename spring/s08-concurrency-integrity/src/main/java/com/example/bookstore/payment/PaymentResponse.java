package com.example.bookstore.payment;

import java.time.Instant;

/**
 * A payment as the API shows it.
 *
 * @param amount in won
 */
public record PaymentResponse(Long id, Long orderId, long amount, PaymentStatus status, Instant createdAt) {

    static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrder().getId(), payment.getAmount(),
                payment.getStatus(), payment.getCreatedAt());
    }
}
