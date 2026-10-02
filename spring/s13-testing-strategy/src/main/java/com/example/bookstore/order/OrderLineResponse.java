package com.example.bookstore.order;

/**
 * One line of an {@link OrderResponse}.
 *
 * @param title     the book's title
 * @param unitPrice the book's price in won when it was ordered
 * @param amount    unit price × quantity, in won
 */
public record OrderLineResponse(long bookId, String title, int quantity, long unitPrice, long amount) {

    static OrderLineResponse from(OrderLine line) {
        return new OrderLineResponse(line.getBook().getId(), line.getBook().getTitle(), line.getQuantity(),
                line.getUnitPrice(), line.getAmount());
    }
}
