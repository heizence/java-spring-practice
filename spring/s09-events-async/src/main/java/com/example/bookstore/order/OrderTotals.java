package com.example.bookstore.order;

/**
 * A number of orders and the sum of their totals.
 *
 * @param amount in won
 */
public record OrderTotals(long orders, long amount) {
}
