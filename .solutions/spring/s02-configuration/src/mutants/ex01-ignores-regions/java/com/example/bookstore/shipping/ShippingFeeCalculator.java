package com.example.bookstore.shipping;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Mutant: binds the supported regions but never checks them. */
@Component
@RequiredArgsConstructor
public class ShippingFeeCalculator {

    public static final ZoneId WAREHOUSE_ZONE = ZoneId.of("Asia/Seoul");

    private final ShippingProperties properties;

    public long feeFor(long subtotal, Region region) {
        Objects.requireNonNull(region, "region");
        if (subtotal < 0) {
            throw new IllegalArgumentException("The subtotal must not be negative, but was %d.".formatted(subtotal));
        }
        return subtotal >= properties.freeThreshold() ? 0 : properties.fee();
    }

    public LocalDate shipDate(Instant orderedAt) {
        ZonedDateTime placed = orderedAt.atZone(WAREHOUSE_ZONE);
        LocalDate day = placed.toLocalDate();
        Duration sinceMidnight = Duration.between(day.atStartOfDay(WAREHOUSE_ZONE), placed);
        return sinceMidnight.compareTo(properties.cutoffTime()) < 0 ? day : day.plusDays(1);
    }
}
