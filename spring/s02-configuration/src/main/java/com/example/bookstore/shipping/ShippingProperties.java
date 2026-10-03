package com.example.bookstore.shipping;

import java.time.Duration;
import java.util.Set;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The bookstore's shipping rules, from the configuration properties under {@code bookstore.shipping}:
 *
 * <pre>
 * bookstore:
 *   shipping:
 *     free-threshold: 20000
 *     fee: 3000
 *     cutoff-time: 14h
 *     supported-regions: seoul, incheon, gyeonggi, busan
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code @ConfigurationProperties} record with the prefix {@code bookstore.shipping}. Spring Boot fills it from
 *       {@code application.yml}, environment variables and command-line arguments.</li>
 *   <li>{@code application.yml} holds the values above.</li>
 *   <li>It's validated at startup ({@code @Validated}), so an invalid value stops the application with an error that
 *       names the property: {@code freeThreshold} must be at least 0, {@code fee} greater than 0, {@code cutoffTime}
 *       must be set, and {@code supportedRegions} must not be empty. A name that isn't a {@link Region} stops the
 *       application too.</li>
 * </ul>
 *
 * @param freeThreshold    orders with a subtotal of at least this many won ship for free
 * @param fee              the shipping fee in won for smaller orders
 * @param cutoffTime       the time of day, measured from midnight in Seoul, before which an order ships the same day;
 *                         {@code 14h} means 2 p.m.
 * @param supportedRegions the regions the bookstore ships to
 */
// Spring Boot binds a record through its constructor (constructor binding): the values arrive once, and the object
// never changes afterwards. @Validated makes Spring Boot run Bean Validation on it at startup, before any bean can
// use a broken value.
@ConfigurationProperties("bookstore.shipping")
@Validated
public record ShippingProperties(
        @PositiveOrZero long freeThreshold,   // 0 is a valid choice: every order ships for free
        @Positive long fee,                   // a missing fee binds as 0, which this rejects
        @NotNull Duration cutoffTime,         // an object: a missing value is null, which @NotNull reports
        @NotEmpty Set<Region> supportedRegions) {

    public ShippingProperties {
        // A defensive copy: nobody can change the bound set later. null stays null, for @NotEmpty to report.
        supportedRegions = supportedRegions == null ? null : Set.copyOf(supportedRegions);
    }
}
