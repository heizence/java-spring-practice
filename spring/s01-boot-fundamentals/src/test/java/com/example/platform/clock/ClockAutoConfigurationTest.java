package com.example.platform.clock;

import static com.example.bookstore.support.ContextAssertions.assertNoBean;
import static com.example.bookstore.support.ContextAssertions.assertStartupFailed;
import static com.example.bookstore.support.ContextAssertions.singleBean;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Tests the auto-configuration the way Spring Boot tests its own: an {@link ApplicationContextRunner} applies it with
 * {@code AutoConfigurations.of(...)}, after the "application's" beans, and each test varies the properties or the
 * application's beans.
 */
@DisplayName("S01-06 · Write your own auto-configuration")
class ClockAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ClockAutoConfiguration.class));

    /** An application that switches auto-configuration on, as {@code @SpringBootApplication} does. */
    @EnableAutoConfiguration
    static class AnyApplication {
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("provides a Clock that tells the current time in UTC")
        void providesAUtcClock() {
            runner.run(context -> {
                Clock clock = singleBean(context, Clock.class);
                Instant before = Instant.now().minus(Duration.ofSeconds(1));

                assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
                assertThat(clock.instant()).isBetween(before, Instant.now().plus(Duration.ofSeconds(1)));
            });
        }

        @Test
        @DisplayName("the default clock ticks in whole microseconds")
        void ticksInMicrosecondsByDefault() {
            runner.run(context -> {
                Clock clock = singleBean(context, Clock.class);

                for (int reading = 0; reading < 100; reading++) {
                    assertThat(clock.instant().getNano() % 1_000).as("nanoseconds of reading %d", reading).isZero();
                }
            });
        }

        @Test
        @DisplayName("platform.clock.zone sets the clock's time zone")
        void zoneProperty() {
            runner.withPropertyValues("platform.clock.zone=Asia/Seoul").run(context -> assertThat(
                    singleBean(context, Clock.class).getZone()).isEqualTo(ZoneId.of("Asia/Seoul")));
        }

        @Test
        @DisplayName("backs off when the application has a Clock of its own, such as a test's TestClock")
        void backsOff() {
            var ownClock = new TestClock();

            runner.run(context -> assertThat(singleBean(context, Clock.class))
                    .as("the clock without one of the application's own")
                    .isNotInstanceOf(TestClock.class));

            runner.withBean("applicationClock", TestClock.class, () -> ownClock).run(context -> assertThat(
                    singleBean(context, Clock.class)).as("the clock with the application's own").isSameAs(ownClock));
        }

        @Test
        @DisplayName("is listed in META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
        void isRegistered() {
            // Every auto-configuration listed in an AutoConfiguration.imports file on the classpath, Spring Boot's too.
            ImportCandidates candidates = ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader());

            assertThat(candidates.getCandidates().contains(ClockAutoConfiguration.class.getName()))
                    .as("ClockAutoConfiguration is listed in an AutoConfiguration.imports file")
                    .isTrue();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("an application with @EnableAutoConfiguration gets the clock without mentioning it")
        void foundThroughEnableAutoConfiguration() {
            new ApplicationContextRunner().withUserConfiguration(AnyApplication.class)
                    .run(context -> assertThat(singleBean(context, Clock.class).getZone()).isEqualTo(ZoneOffset.UTC));
        }

        @Test
        @DisplayName("platform.clock.precision sets how the clock ticks")
        void precisionProperty() {
            runner.withPropertyValues("platform.clock.precision=1ms").run(context -> {
                Clock clock = singleBean(context, Clock.class);

                for (int reading = 0; reading < 100; reading++) {
                    assertThat(clock.instant().getNano() % 1_000_000).as("nanoseconds of reading %d", reading)
                            .isZero();
                }
            });
        }

        @Test
        @DisplayName("platform.clock.enabled=false switches it off")
        void canBeSwitchedOff() {
            runner.run(context -> singleBean(context, Clock.class));

            runner.withPropertyValues("platform.clock.enabled=false")
                    .run(context -> assertNoBean(context, Clock.class));
        }

        @Test
        @DisplayName("platform.clock.enabled=true leaves it on")
        void staysOnWhenEnabled() {
            runner.withPropertyValues("platform.clock.enabled=true").run(context -> singleBean(context, Clock.class));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an unknown time zone stops the application with an error that names the property")
        void unknownZone() {
            runner.withPropertyValues("platform.clock.zone=Mars/Olympus")
                    .run(context -> assertThat(assertStartupFailed(context)).hasMessageContaining("platform.clock"));
        }

        @Test
        @DisplayName("a negative precision stops the application")
        void negativePrecision() {
            runner.withPropertyValues("platform.clock.precision=-1s").run(context -> assertStartupFailed(context));
        }
    }
}
