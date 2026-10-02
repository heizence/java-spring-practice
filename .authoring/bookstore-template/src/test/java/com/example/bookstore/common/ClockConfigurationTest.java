package com.example.bookstore.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Template · The application's clock")
class ClockConfigurationTest {

    private final Clock clock = new ClockConfiguration().clock();

    @Test
    @DisplayName("tells the current time in UTC")
    void tellsTheCurrentTimeInUtc() {
        Instant before = Instant.now().minus(Duration.ofSeconds(1));

        Instant now = clock.instant();

        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(now).isBetween(before, Instant.now().plus(Duration.ofSeconds(1)));
    }

    @Test
    @DisplayName("has whole microseconds, like the DATETIME(6) columns")
    void hasWholeMicroseconds() {
        for (int i = 0; i < 100; i++) {
            assertThat(clock.instant().getNano() % 1_000).as("nanoseconds of reading %d", i).isZero();
        }
    }
}
