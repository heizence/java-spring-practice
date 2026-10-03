package com.example.bookstore.order;

import java.time.Instant;
import java.util.List;

/**
 * An order as the API shows it.
 *
 * @param totalAmount the sum of the lines in won
 * @param orderedAt   when the member placed the order
 */
public record OrderResponse(Long id, Long memberId, OrderStatus status, long totalAmount, Instant orderedAt,
                            List<Line> lines) {

    /**
     * One book of the order.
     *
     * @param unitPrice the book's price in won when it was ordered
     */
    public record Line(Long bookId, int quantity, long unitPrice) {
    }

    /** Reads the order's lines, so call it while the order's persistence context is open. */
    static OrderResponse from(Order order) {
        List<Line> lines = order.getLines().stream()
                .map(line -> new Line(line.getBook().getId(), line.getQuantity(), line.getUnitPrice()))
                .toList();
        return new OrderResponse(order.getId(), order.getMember().getId(), order.getStatus(), order.getTotalAmount(),
                order.getOrderedAt(), lines);
    }
}
