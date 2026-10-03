package com.example.bookstore.common;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link Clock} that the application reads the current time from.
 *
 * <p>Code that needs "now" injects this bean and calls {@code clock.instant()} instead of {@code Instant.now()}.
 * Tests replace the bean with a clock they control, so time-dependent rules (a coupon's validity period, for
 * example) give the same result on every run.
 */
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    /**
     * The system clock in UTC, cut to whole microseconds. Timestamps are stored in {@code DATETIME(6)} columns,
     * which keep microseconds; on Linux the system clock also has nanoseconds, which MySQL would round away. Cutting
     * them here keeps the time in an entity equal to the time stored in its row.
     */
    @Bean
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
