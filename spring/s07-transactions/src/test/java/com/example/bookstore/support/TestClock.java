package com.example.bookstore.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * A {@link Clock} that stands still until a test moves it. In integration tests it replaces the application's clock
 * (see {@link IntegrationTestConfiguration}), so "now" is the same on every run, whatever the real date is.
 *
 * <p>Every test starts at {@link #DEFAULT_INSTANT}. A test that needs another time sets it, and
 * {@link IntegrationTestListener} puts the clock back before the next test:
 *
 * <pre>{@code
 * @Autowired TestClock clock;
 *
 * @Test
 * void rejectsAnExpiredCoupon() {
 *     clock.setInstant(Instant.parse("2026-11-01T00:00:00Z"));
 *     ...
 * }
 * }</pre>
 *
 * <p>The clock is safe to read from other threads (for example an {@code @Async} method) while a test changes it.
 */
public final class TestClock extends Clock {

    /**
     * Where every test starts: 2026-10-15T00:00:00Z. At this moment the seeded coupons {@code WELCOME5000},
     * {@code FLASH10000} and {@code AUTUMN3000} are valid, {@code EXPIRED2000} has expired, and every seeded order
     * lies in the past.
     */
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
