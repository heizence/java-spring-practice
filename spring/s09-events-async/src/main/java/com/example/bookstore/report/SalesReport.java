package com.example.bookstore.report;

import java.time.Instant;
import java.time.YearMonth;

/**
 * The sales of one month.
 *
 * @param month       a calendar month, in UTC
 * @param paidOrders  the orders of that month that were paid ({@code PAID} or {@code SHIPPED})
 * @param revenue     the sum of their totals, in won
 * @param generatedAt when the report was made
 */
public record SalesReport(YearMonth month, long paidOrders, long revenue, Instant generatedAt) {
}
