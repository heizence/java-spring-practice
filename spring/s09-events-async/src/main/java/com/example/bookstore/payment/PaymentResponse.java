package com.example.bookstore.payment;

/**
 * A payment as the API shows it.
 *
 * @param amount in won
 */
public record PaymentResponse(Long id, Long orderId, long amount, PaymentStatus status) {

    /** Reads only the order's id, which doesn't load the order. */
    static PaymentResponse from(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrder().getId(), payment.getAmount(),
                payment.getStatus());
    }
}
