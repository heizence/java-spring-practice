package com.example.bookstore.payment;

import java.time.Instant;

import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderStatus;

/**
 * A payment as the receipt page shows it, with the status of the order it paid for.
 *
 * @param amount    in won
 * @param createdAt when the payment was made
 */
public record PaymentResponse(Long id, Long orderId, OrderStatus orderStatus, long amount, PaymentStatus status,
                              Instant createdAt) {

    /** Reads the payment's order, which Hibernate loads lazily. */
    static PaymentResponse from(Payment payment) {
        Order order = payment.getOrder();
        return new PaymentResponse(payment.getId(), order.getId(), order.getStatus(), payment.getAmount(),
                payment.getStatus(), payment.getCreatedAt());
    }
}
