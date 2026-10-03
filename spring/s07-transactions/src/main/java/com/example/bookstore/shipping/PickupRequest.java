package com.example.bookstore.shipping;

import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderLine;

/**
 * What the courier needs to know to collect an order's parcel.
 *
 * @param recipient the name of the member who ordered
 * @param copies    how many books are in the parcel
 */
public record PickupRequest(long orderId, String recipient, int copies) {

    /** Reads the order's member and lines, so call it while the order's persistence context is open. */
    static PickupRequest of(Order order) {
        int copies = order.getLines().stream().mapToInt(OrderLine::getQuantity).sum();
        return new PickupRequest(order.getId(), order.getMember().getName(), copies);
    }
}
