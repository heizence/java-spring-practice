package com.example.bookstore.shipping;

import java.time.Duration;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Mutant: constraints without @Validated, so Spring Boot never checks them. */
@ConfigurationProperties("bookstore.shipping")
public record ShippingProperties(
        @PositiveOrZero long freeThreshold,
        @Positive long fee,
        @NotNull Duration cutoffTime,
        @NotEmpty Set<Region> supportedRegions) {

    public ShippingProperties {
        supportedRegions = supportedRegions == null ? null : Set.copyOf(supportedRegions);
    }
}
