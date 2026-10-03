package com.example.bookstore.support;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.example.bookstore.common.timing.Ticker;

/**
 * A {@link Ticker} that stands still until a test moves it, so that every measured duration is exact. In integration
 * tests it replaces the application's ticker (see {@link IntegrationTestConfiguration}), and
 * {@link IntegrationTestListener} puts it back to 0 before each test.
 *
 * <pre>{@code
 * @Timed
 * public Report monthlySales() {
 *     ticker.advance(Duration.ofMillis(250)); // "the work takes 250 ms"
 *     ...
 * }
 * }</pre>
 *
 * <p>It is safe to use from several threads.
 */
public final class FakeTicker implements Ticker {

    private final AtomicLong nanos = new AtomicLong();

    @Override
    public long read() {
        return nanos.get();
    }

    /** Moves the ticker forward. */
    public void advance(Duration duration) {
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("A ticker never goes back: " + duration);
        }
        nanos.addAndGet(duration.toNanos());
    }

    /** Puts the ticker back to 0. */
    public void reset() {
        nanos.set(0);
    }
}
