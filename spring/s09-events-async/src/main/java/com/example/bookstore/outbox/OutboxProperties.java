package com.example.bookstore.outbox;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the outbox relay ({@code bookstore.outbox.*}).
 *
 * @param relayInterval the pause between two runs of the relay, and before its first run
 * @param batchSize     the most events one run publishes
 */
@Validated
@ConfigurationProperties(prefix = "bookstore.outbox")
public record OutboxProperties(@NotNull Duration relayInterval, @Positive int batchSize) {
}
