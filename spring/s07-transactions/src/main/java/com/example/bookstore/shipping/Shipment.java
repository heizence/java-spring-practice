package com.example.bookstore.shipping;

/**
 * An order on its way to the customer.
 *
 * @param trackingNumber the courier's number for the parcel
 */
public record Shipment(long orderId, String trackingNumber) {
}
