package com.example.bookstore.shipping;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Works out an order's shipping fee and shipping date from the configured {@link ShippingProperties}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>It's a bean ({@code @Component}) that gets {@link ShippingProperties} through its constructor, like any other
 *       dependency.</li>
 *   <li>{@link #feeFor}: 0 for a subtotal of at least {@code freeThreshold}, otherwise {@code fee}. A region that isn't
 *       in {@code supportedRegions} throws {@link IllegalArgumentException} with the region in the message; so does a
 *       negative subtotal. A {@code null} region throws {@link NullPointerException}.</li>
 *   <li>{@link #shipDate}: an order placed before {@code cutoffTime} ships the same day; from the cutoff on, it ships
 *       the next day. Both the time and the day are those of the warehouse, in {@link #WAREHOUSE_ZONE}: an order at
 *       20:00 UTC is placed at 05:00 the next morning in Seoul. A {@code null} time throws
 *       {@link NullPointerException}.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ShippingFeeCalculator {

    /** The warehouse's time zone, which decides what "today" and "before the cutoff" mean. */
    public static final ZoneId WAREHOUSE_ZONE = ZoneId.of("Asia/Seoul");

    // The properties are a bean like any other: inject the record, not single values with @Value.
    private final ShippingProperties properties;

    /** The shipping fee in won for an order with this subtotal, delivered to this region. */
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

    /** The day, in the warehouse's time zone, on which an order placed at {@code orderedAt} ships. */
    public LocalDate shipDate(Instant orderedAt) {
        ZonedDateTime placed = orderedAt.atZone(WAREHOUSE_ZONE);
        LocalDate day = placed.toLocalDate();
        // Time since midnight as a Duration, so that it compares directly with the configured cutoff time.
        // atStartOfDay(zone) also handles days with a daylight saving change correctly: Korea has none today, but the
        // code doesn't depend on that.
        Duration sinceMidnight = Duration.between(day.atStartOfDay(WAREHOUSE_ZONE), placed);
        return sinceMidnight.compareTo(properties.cutoffTime()) < 0 ? day : day.plusDays(1);
    }
}
