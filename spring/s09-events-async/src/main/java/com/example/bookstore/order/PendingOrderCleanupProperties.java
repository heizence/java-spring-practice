package com.example.bookstore.order;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the job that cancels unpaid orders ({@code bookstore.orders.pending-cleanup.*}).
 *
 * @param maxAge   how long an order may stay {@code PENDING} before the job cancels it
 * @param interval the pause between two runs of the job, and before its first run
 */
@Validated
@ConfigurationProperties(prefix = "bookstore.orders.pending-cleanup")
public record PendingOrderCleanupProperties(@NotNull Duration maxAge, @NotNull Duration interval) {
}
