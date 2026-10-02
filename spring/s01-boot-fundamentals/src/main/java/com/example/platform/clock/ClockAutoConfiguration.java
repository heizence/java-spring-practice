package com.example.platform.clock;

import java.time.Clock;

/**
 * Gives every application that has the platform library on its classpath the company's standard {@link Clock} bean.
 *
 * <p>The package {@code com.example.platform} plays a separate library: a jar that a platform team publishes and that
 * every service adds as a dependency. That's why it isn't under {@code com.example.bookstore}. Component scanning
 * must never find an auto-configuration; Spring Boot finds it through the file
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} instead.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>An {@code @AutoConfiguration} class, listed in {@code AutoConfiguration.imports}. It binds
 *       {@link ClockProperties} with {@code @EnableConfigurationProperties}.</li>
 *   <li>{@link #clock(ClockProperties)} is a {@code @Bean} method: the system clock in the configured zone, ticking
 *       in whole multiples of the configured precision ({@link Clock#tick}). With the defaults, that's the clock of
 *       {@code ClockConfiguration} in S01-02: UTC, in whole microseconds.</li>
 *   <li>It backs off when the application has a {@code Clock} bean of its own, such as a test's fixed clock.</li>
 *   <li>{@code platform.clock.enabled=false} switches it off: then it provides no clock at all. Without the property,
 *       it's on.</li>
 *   <li>An invalid setting, such as an unknown time zone, stops the application at startup. Typed properties do that
 *       for you: Spring can't turn {@code Mars/Olympus} into a {@code ZoneId}.</li>
 * </ul>
 */
public class ClockAutoConfiguration {

    /** The platform clock: the system clock in the configured zone, ticking in the configured precision. */
    public Clock clock(ClockProperties properties) {
        // TODO: S01-06
        throw new UnsupportedOperationException("TODO: S01-06");
    }
}
