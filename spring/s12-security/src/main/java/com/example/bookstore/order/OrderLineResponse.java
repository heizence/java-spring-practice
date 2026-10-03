package com.example.bookstore.order;

/**
 * One line of an order as the API shows it.
 *
 * @param unitPrice the book's price in won when it was ordered
 * @param amount    unit price × quantity
 */
public record OrderLineResponse(Long bookId, String title, int quantity, long unitPrice, long amount) {

    /** Reads the line's book, so call it while the order's persistence context is open. */
    static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getBook().getId(), line.getBook().getTitle(), line.getQuantity(),
                line.getUnitPrice(), line.getAmount());
    }
}
