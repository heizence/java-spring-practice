package com.example.bookstore.order;

import java.time.Instant;
import java.util.List;

/** An order with its lines, as the API shows it. */
public record OrderResponse(Long id, OrderStatus status, long totalAmount, Instant orderedAt,
                            List<OrderLineResponse> lines) {

    /** Reads the order's lines and their books, so call it while the order's persistence context is open. */
    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), order.getOrderedAt(),
                order.getLines().stream().map(OrderLineResponse::from).toList());
    }
}
