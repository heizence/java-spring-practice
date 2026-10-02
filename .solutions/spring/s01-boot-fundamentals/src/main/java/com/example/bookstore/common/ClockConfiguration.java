package com.example.bookstore.common;

import java.time.Clock;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provides the {@link Clock} that the application reads the current time from.
 *
 * <p>Code that needs "now" injects this bean and calls {@code clock.instant()} instead of {@code Instant.now()}.
 * Tests replace the bean with a clock they control, so time-dependent rules (a coupon's validity period, for example)
 * give the same result on every run.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code @Configuration} class that component scanning finds. It doesn't need the proxy that
 *       {@code @Configuration} adds by default, because its {@code @Bean} method calls no other {@code @Bean}
 *       method: say so with {@code proxyBeanMethods = false}.</li>
 *   <li>{@link #clock()} is a {@code @Bean} method. {@code Clock} is a JDK class that you can't annotate with
 *       {@code @Component}, so a {@code @Bean} method is the way to make it a bean.</li>
 *   <li>The clock is the system clock in UTC, cut to whole microseconds:
 *       {@code Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))}. From S05 on, times are stored in MySQL
 *       {@code DATETIME(6)} columns, which keep microseconds. The system clock can have nanoseconds, which MySQL
 *       would round away; cutting them here keeps a time in memory equal to the time stored in its row.</li>
 * </ul>
 */
// proxyBeanMethods = false: no CGLIB subclass. A proxy only matters when one @Bean method calls another and must get
// the same singleton back; here there's nothing to call, and startup gets a little cheaper.
@Configuration(proxyBeanMethods = false)
public class ClockConfiguration {

    /** The system clock in UTC, in whole microseconds. */
    @Bean // the bean's name is the method's name: "clock"
    public Clock clock() {
        return Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000));
    }
}
