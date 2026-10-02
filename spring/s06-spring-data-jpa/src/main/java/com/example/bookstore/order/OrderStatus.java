package com.example.bookstore.order;

/**
 * Where an order is in its life cycle: {@code PENDING → PAID → SHIPPED}, or {@code CANCELLED} before it ships.
 * Stored by name in {@code orders.status}.
 */
public enum OrderStatus {

    /** Placed, not paid yet. Its lines can still change. */
    PENDING,

    /** Paid; waiting to be shipped. */
    PAID,

    /** On its way to the customer. Final. */
    SHIPPED,

    /** Cancelled before shipping; a paid order is refunded. Final. */
    CANCELLED;

    /** Whether an order in this status may move to {@code next}. */
    public boolean canChangeTo(OrderStatus next) {
        return switch (this) {
            case PENDING -> next == PAID || next == CANCELLED;
            case PAID -> next == SHIPPED || next == CANCELLED;
            case SHIPPED, CANCELLED -> false;
        };
    }
}
