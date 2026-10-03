package com.example.bookstore.common.timing;

/**
 * A time source for measuring durations, in nanoseconds. Only the difference between two readings means something:
 * {@code ticker.read() - start} is how long something took.
 *
 * <p>Why not the {@code Clock} bean? A {@code Clock} tells the time of day, which can jump: when the computer
 * synchronizes its clock, a call could seem to take minutes, or less than nothing. {@link System#nanoTime()} never
 * jumps, so it's the right tool for durations ({@link #system()}). Tests replace the ticker with a fake one that moves
 * only when the test says so, which makes every duration exact. Caffeine, the cache library of S10, works the same
 * way.
 */
@FunctionalInterface
public interface Ticker {

    /** Nanoseconds since an arbitrary starting point, which stays the same while the application runs. */
    long read();

    /** The ticker the application uses: {@link System#nanoTime()}. */
    static Ticker system() {
        return System::nanoTime;
    }
}
