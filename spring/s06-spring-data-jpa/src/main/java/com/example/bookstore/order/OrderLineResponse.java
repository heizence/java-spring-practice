package com.example.bookstore.order;

/**
 * One line of an {@link OrderResponse}.
 *
 * @param title     the book's title
 * @param unitPrice the book's price in won when it was ordered
 * @param amount    unit price × quantity
 */
public record OrderLineResponse(Long bookId, String title, int quantity, long unitPrice, long amount) {

    /** Reads the line's book, which Hibernate loads lazily. */
    static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getBook().getId(), line.getBook().getTitle(), line.getQuantity(),
                line.getUnitPrice(), line.getAmount());
    }
}
