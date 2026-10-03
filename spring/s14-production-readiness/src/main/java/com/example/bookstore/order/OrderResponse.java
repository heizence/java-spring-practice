package com.example.bookstore.order;

import java.time.Instant;
import java.util.List;

/** An order with its lines, as the API shows it. */
public record OrderResponse(Long id, Long memberId, OrderStatus status, long totalAmount, Instant orderedAt,
                            List<Line> lines) {

    /**
     * One line of the order.
     *
     * @param unitPrice the book's price in won when it was ordered
     */
    public record Line(Long bookId, int quantity, long unitPrice) {
    }

    /** Reads the order's member id and lines, so call it while the order's persistence context is open. */
    static OrderResponse from(Order order) {
        return new OrderResponse(order.getId(), order.getMember().getId(), order.getStatus(), order.getTotalAmount(),
                order.getOrderedAt(), order.getLines().stream()
                        .map(line -> new Line(line.getBook().getId(), line.getQuantity(), line.getUnitPrice()))
                        .toList());
    }
}
