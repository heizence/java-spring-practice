package com.example.bookstore.shipping;

import java.time.Duration;
import java.util.Set;

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
public record ShippingProperties(long freeThreshold, long fee, Duration cutoffTime, Set<Region> supportedRegions) {
}
