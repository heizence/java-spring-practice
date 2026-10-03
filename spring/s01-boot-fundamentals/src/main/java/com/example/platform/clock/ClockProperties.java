package com.example.platform.clock;

import java.time.Duration;
import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The settings of {@link ClockAutoConfiguration}, under the prefix {@code platform.clock}. Spring Boot fills this
 * record from {@code application.yml}, environment variables or command-line arguments, and converts each value to
 * the component's type. S02 covers configuration properties in depth.
 *
 * <pre>
 * platform:
 *   clock:
 *     zone: Asia/Seoul
 *     precision: 1ms
 * </pre>
 *
 * @param enabled   whether {@link ClockAutoConfiguration} provides a clock at all; {@code true} when not set
 * @param zone      the clock's time zone; {@code UTC} when not set
 * @param precision the clock ticks in whole multiples of this duration; one microsecond ({@code 1us}) when not set,
 *                  which is the precision of MySQL's {@code DATETIME(6)} columns
 */
@ConfigurationProperties("platform.clock")
public record ClockProperties(
        @DefaultValue("true") boolean enabled,
        @DefaultValue("UTC") ZoneId zone,
        @DefaultValue("1us") Duration precision) {
}
