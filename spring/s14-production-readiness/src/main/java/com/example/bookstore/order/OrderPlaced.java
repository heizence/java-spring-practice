package com.example.bookstore.order;

/** Published by {@link OrderService#placeOrder} when an order was placed. */
public record OrderPlaced(long orderId) {
}
