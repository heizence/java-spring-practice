package practice.j07.ex01;

import java.time.Duration;
import java.util.Objects;
import java.util.StringJoiner;

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
        Objects.requireNonNull(duration, "duration");
        if (duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be negative, but was " + duration);
        }
        // The xxxPart() methods return one unit of the duration "on the clock": toHoursPart() is 0..23 and
        // toMinutesPart() 0..59. toHours() and toMinutes() would return the whole duration in that unit instead.
        // toSecondsPart() drops the nanoseconds, which is the rule for fractions of a second.
        var parts = new StringJoiner(" ").setEmptyValue("0s");
        addPart(parts, duration.toDaysPart(), "d");
        addPart(parts, duration.toHoursPart(), "h");
        addPart(parts, duration.toMinutesPart(), "m");
        addPart(parts, duration.toSecondsPart(), "s");
        return parts.toString();
    }

    private static void addPart(StringJoiner parts, long value, String unit) {
        if (value != 0) {
            parts.add(value + unit);
        }
    }
}
