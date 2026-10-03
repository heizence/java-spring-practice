package com.example.bookstore.shipping;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the simulated courier ({@code bookstore.courier.*} in {@code application.yml}).
 *
 * @param latency how long each call to the courier takes
 */
@Validated
@ConfigurationProperties(prefix = "bookstore.courier")
public record CourierProperties(@NotNull Duration latency) {
}
