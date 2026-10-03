package com.example.bookstore.order;

import java.time.Instant;

/**
 * An order in a list of orders. It has no lines, so that a page of orders loads with one query instead of one more
 * query per order.
 *
 * @param totalAmount the order's total in won
 * @param orderedAt   when the member placed the order
 */
public record OrderSummaryResponse(long id, OrderStatus status, long totalAmount, Instant orderedAt) {

    static OrderSummaryResponse from(Order order) {
        return new OrderSummaryResponse(order.getId(), order.getStatus(), order.getTotalAmount(),
                order.getOrderedAt());
    }
}
