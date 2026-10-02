package com.example.bookstore.shipping;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Mutant: compares the cutoff with the time of day in UTC instead of Seoul. */
@Component
@RequiredArgsConstructor
public class ShippingFeeCalculator {

    public static final ZoneId WAREHOUSE_ZONE = ZoneId.of("Asia/Seoul");

    private final ShippingProperties properties;

    public long feeFor(long subtotal, Region region) {
        Objects.requireNonNull(region, "region");
        if (!properties.supportedRegions().contains(region)) {
            throw new IllegalArgumentException("The bookstore doesn't ship to %s.".formatted(region));
        }
        if (subtotal < 0) {
            throw new IllegalArgumentException("The subtotal must not be negative, but was %d.".formatted(subtotal));
        }
        return subtotal >= properties.freeThreshold() ? 0 : properties.fee();
    }

    public LocalDate shipDate(Instant orderedAt) {
        ZonedDateTime placed = orderedAt.atZone(ZoneOffset.UTC);
        LocalDate day = placed.toLocalDate();
        Duration sinceMidnight = Duration.between(day.atStartOfDay(ZoneOffset.UTC), placed);
        return sinceMidnight.compareTo(properties.cutoffTime()) < 0 ? day : day.plusDays(1);
    }
}
