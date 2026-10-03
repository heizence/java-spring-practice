package com.example.bookstore.order;

import java.time.Instant;
import java.util.List;

/**
 * An order as the API shows it, with its lines.
 *
 * @param totalAmount the sum of the lines' amounts in won
 * @param orderedAt   when the member placed the order
 */
public record OrderResponse(long id, long memberId, OrderStatus status, long totalAmount, Instant orderedAt,
                            List<OrderLineResponse> lines) {

    public OrderResponse {
        lines = List.copyOf(lines);
    }

    /** Reads the order's lines and their books, so call it while the order's persistence context is open. */
    static OrderResponse from(Order order) {
        // getMember().getId() doesn't load the member: a lazy proxy knows its own id.
        return new OrderResponse(order.getId(), order.getMember().getId(), order.getStatus(), order.getTotalAmount(),
                order.getOrderedAt(), order.getLines().stream().map(OrderLineResponse::from).toList());
    }
}
