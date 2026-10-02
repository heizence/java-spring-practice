package practice.j07.ex01;

import java.time.Duration;

/**
 * Formats durations for people, for example in "Build finished in 1h 5m".
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The units are {@code d} (days of exactly 24 hours), {@code h}, {@code m} and {@code s}. Every unit whose value
 *       isn't zero is shown, largest first, separated by single spaces: {@code "1d 2h 5m 30s"}, {@code "1h 5s"},
 *       {@code "45s"}.</li>
 *   <li>Fractions of a second are dropped: 1.9 seconds is {@code "1s"}.</li>
 *   <li>A duration shorter than one second, including zero, is {@code "0s"}.</li>
 *   <li>A negative duration throws {@link IllegalArgumentException}. {@code null} throws
 *       {@link NullPointerException}.</li>
 * </ul>
 */
public final class Durations {

    private Durations() {
    }

    /** Returns {@code duration} as short text, such as {@code "1h 5m"}. */
    public static String humanize(Duration duration) {
        // TODO: J07-01
        throw new UnsupportedOperationException("TODO: J07-01");
    }
}
