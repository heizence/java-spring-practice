package com.example.bookstore.payment;

/** The outcome of a {@link Payment}. Stored by name in {@code payments.status}. */
public enum PaymentStatus {

    /** The money was taken from the member's wallet. */
    APPROVED,

    /** The payment didn't go through, for example because the balance was too low. Kept as a record. */
    FAILED,

    /** An approved payment whose money was returned, because the order was cancelled. */
    REFUNDED
}
