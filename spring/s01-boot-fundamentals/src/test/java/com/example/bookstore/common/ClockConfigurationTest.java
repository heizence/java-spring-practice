package com.example.bookstore.common;

import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;

/** Tests the {@code Clock} bean in a context that holds only what component scanning finds in this package. */
@DisplayName("S01-02 · Beans and constructor injection")
class ClockConfigurationTest {

    /** Scans {@code com.example.bookstore.common} the way {@code @SpringBootApplication} scans the application. */
    @ComponentScan(basePackageClasses = ClockConfiguration.class)
    static class CommonPackage {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(CommonPackage.class);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("ClockConfiguration provides a Clock bean named \"clock\", found by component scanning")
        void providesTheClockBean() {
            runner.run(context -> {
                singleBean(context, Clock.class);

                assertThat(context.getBeanNamesForType(Clock.class)).containsExactly("clock");
            });
        }

        @Test
        @DisplayName("the clock tells the current time, in UTC")
        void tellsTheCurrentTimeInUtc() {
            runner.run(context -> {
                Clock clock = singleBean(context, Clock.class);
                Instant before = Instant.now().minus(Duration.ofSeconds(1));

                Instant now = clock.instant();

                assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
                assertThat(now).isBetween(before, Instant.now().plus(Duration.ofSeconds(1)));
            });
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the clock ticks in whole microseconds, like DATETIME(6) columns")
        void ticksInWholeMicroseconds() {
            runner.run(context -> {
                Clock clock = singleBean(context, Clock.class);

                for (int reading = 0; reading < 100; reading++) {
                    assertThat(clock.instant().getNano() % 1_000).as("nanoseconds of reading %d", reading).isZero();
                }
            });
        }

        @Test
        @DisplayName("ClockConfiguration isn't wrapped in a proxy subclass (proxyBeanMethods = false)")
        void isNotProxied() {
            runner.run(context -> assertThat(singleBean(context, ClockConfiguration.class).getClass())
                    .as("the class of the ClockConfiguration bean")
                    .isEqualTo(ClockConfiguration.class));
        }
    }
}
