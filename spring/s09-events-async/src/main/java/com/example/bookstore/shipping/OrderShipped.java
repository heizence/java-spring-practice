package com.example.bookstore.shipping;

import java.time.Instant;

/**
 * The event that the delivery-tracking service receives when an order ships. It travels as JSON in an outbox event's
 * payload: {@code {"orderId":201,"memberId":1,"shippedAt":"2026-10-15T00:00:00Z"}}.
 */
public record OrderShipped(long orderId, long memberId, Instant shippedAt) {
}
