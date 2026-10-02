package com.example.bookstore.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Template · TestClock")
class TestClockTest {

    private final TestClock clock = new TestClock();

    @Test
    @DisplayName("starts at the default instant, in UTC, and stands still")
    void startsAtTheDefaultInstant() {
        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-10-15T00:00:00Z"));
        assertThat(clock.getZone()).isEqualTo(ZoneOffset.UTC);
        assertThat(clock.instant()).isEqualTo(clock.instant());
    }

    @Test
    @DisplayName("moves to a given instant, forward and back")
    void movesToAnInstant() {
        clock.setInstant(Instant.parse("2027-01-01T00:00:00Z"));
        assertThat(clock.instant()).isEqualTo(Instant.parse("2027-01-01T00:00:00Z"));

        clock.setInstant(Instant.parse("2026-01-01T00:00:00Z"));
        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"));
    }

    @Test
    @DisplayName("advances by a duration")
    void advances() {
        clock.advance(Duration.ofMinutes(90));

        assertThat(clock.instant()).isEqualTo(Instant.parse("2026-10-15T01:30:00Z"));
    }

    @Test
    @DisplayName("reset() goes back to the default instant")
    void resets() {
        clock.advance(Duration.ofDays(3));

        clock.reset();

        assertThat(clock.instant()).isEqualTo(TestClock.DEFAULT_INSTANT);
    }

    @Test
    @DisplayName("a view in another zone shows the same instant and moves with the clock")
    void viewInAnotherZone() {
        Clock seoul = clock.withZone(ZoneId.of("Asia/Seoul"));

        clock.advance(Duration.ofHours(1));

        assertThat(seoul.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
        assertThat(seoul.instant()).isEqualTo(clock.instant());
    }
}
