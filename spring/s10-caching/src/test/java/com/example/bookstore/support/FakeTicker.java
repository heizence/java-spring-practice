package com.example.bookstore.support;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import com.github.benmanes.caffeine.cache.Ticker;

/**
 * A Caffeine {@link Ticker} that stands still until a test moves it. Caffeine reads its ticker, not the
 * {@code Clock} bean, to decide when an entry expires, so the {@link TestClock} can't make cache entries expire. In
 * integration tests this ticker replaces the application's {@code cacheTicker} (see
 * {@link IntegrationTestConfiguration}), and {@link IntegrationTestListener} puts it back to 0 before each test:
 *
 * <pre>{@code
 * @Autowired FakeTicker cacheTicker;
 *
 * @Test
 * void expiresAfterTenMinutes() {
 *     ...
 *     cacheTicker.advance(Duration.ofMinutes(10));
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
