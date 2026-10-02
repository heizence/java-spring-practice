package com.example.bookstore.notification;

/**
 * Tells members about their orders. Which channel it uses (email, or none at all) depends on the configuration: see
 * {@link NotificationConfiguration}.
 */
public interface Notifier {

    /**
     * Tells a member that their order was placed.
     *
     * @param email       the member's email address
     * @param totalAmount what the member paid, in won
     */
    void orderPlaced(String email, long orderId, long totalAmount);
}
