package com.example.bookstore.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Clock} that stands still until a test moves it, so "now" is the same on every run, whatever the real date
 * is. A test that needs it registers it as the primary {@code Clock} bean, which wins over the application's own:
 *
 * <pre>{@code
 * new ApplicationContextRunner()
 *         .withBean(TestClock.class, TestClock::new, definition -> definition.setPrimary(true))
 * }</pre>
 *
 * <p>Every new clock starts at {@link #DEFAULT_INSTANT}. The clock is safe to read from other threads while a test
 * changes it. (This is the bookstore template's {@code TestClock}; modules with a database also have helpers that
 * reset it before each test.)
 */
public final class TestClock extends Clock {

    /** Where every new clock starts: 2026-10-15T00:00:00Z, the same moment as in the modules with a database. */
    public static final Instant DEFAULT_INSTANT = Instant.parse("2026-10-15T00:00:00Z");

    private final AtomicReference<Instant> now;
    private final ZoneId zone;

    /** A clock in UTC, at {@link #DEFAULT_INSTANT}. */
    public TestClock() {
        this(new AtomicReference<>(DEFAULT_INSTANT), ZoneOffset.UTC);
    }

    private TestClock(AtomicReference<Instant> now, ZoneId zone) {
        this.now = now;
        this.zone = zone;
    }

    @Override
    public Instant instant() {
        return now.get();
    }

    @Override
    public ZoneId getZone() {
        return zone;
    }

    /** A view of this clock in another zone. It shows the same instant, and moves when this clock moves. */
    @Override
    public Clock withZone(ZoneId zone) {
        return new TestClock(now, Objects.requireNonNull(zone, "zone"));
    }

    /** Moves the clock to an instant, which may be earlier than the current one. */
    public void setInstant(Instant instant) {
        now.set(Objects.requireNonNull(instant, "instant"));
    }

    /** Moves the clock forward (or backward, for a negative duration). */
    public void advance(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        now.updateAndGet(instant -> instant.plus(duration));
    }

    /** Puts the clock back to {@link #DEFAULT_INSTANT}. */
    public void reset() {
        now.set(DEFAULT_INSTANT);
    }
}
