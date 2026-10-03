package com.example.bookstore.order;

import java.time.Instant;
import java.util.List;

/**
 * An order as the API shows it, with its lines in the order they were added.
 *
 * @param totalAmount the sum of the lines' amounts in won
 * @param orderedAt   when the member placed the order
 */
public record OrderResponse(Long id, Long memberId, OrderStatus status, long totalAmount, Instant orderedAt,
                            List<OrderLineResponse> lines) {

    /**
     * Reads the order's lines and their books, which Hibernate loads lazily. So call it while the order's
     * persistence context is open: inside the transaction that loaded the order.
     */
    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getMember().getId(), order.getStatus(), order.getTotalAmount(),
                order.getOrderedAt(), order.getLines().stream().map(OrderLineResponse::from).toList());
    }
}
