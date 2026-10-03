package com.example.bookstore.order;

import java.time.Instant;
import java.util.List;

/**
 * An order as the API shows it.
 *
 * @param totalAmount the sum of the lines' amounts, in won
 * @param lines       in the order they were added
 */
public record OrderResponse(Long id, OrderStatus status, long totalAmount, Instant orderedAt, List<Line> lines) {

    public OrderResponse {
        lines = List.copyOf(lines);
    }

    /**
     * One line of the order.
     *
     * @param unitPrice the book's price in won when it was ordered
     */
    public record Line(long bookId, int quantity, long unitPrice) {
    }

    /** Reads the order's lines, so call it while the order's persistence context is open. */
    static OrderResponse from(Order order) {
        List<Line> lines = order.getLines().stream()
                .map(line -> new Line(line.getBook().getId(), line.getQuantity(), line.getUnitPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getStatus(), order.getTotalAmount(), order.getOrderedAt(),
                lines);
    }
}
