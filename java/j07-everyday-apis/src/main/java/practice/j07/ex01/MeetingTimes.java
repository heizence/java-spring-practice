package practice.j07.ex01;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

/**
 * Converts and formats meeting times for people in different time zones.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #convert} reads {@code localTime} as a wall-clock time in {@code fromZone} and returns the same moment
 *       in {@code toZone}.</li>
 *   <li>A wall-clock time that doesn't exist in {@code fromZone}, because the clocks jump forward there (a DST gap),
 *       is moved forward by the length of the gap, as calendar apps do: 02:30 on 2026-03-08 in New York becomes
 *       03:30.</li>
 *   <li>A wall-clock time that happens twice, because the clocks go back (a DST overlap), means the earlier of the
 *       two moments: 01:30 on 2026-11-01 in New York is 01:30 EDT (UTC-4), not the second 01:30 EST (UTC-5) an hour
 *       later.</li>
 *   <li>{@link #format} writes a time like {@code "Sun 2026-03-08 16:30 (Asia/Seoul)"}: the short English day name,
 *       the date, the time on a 24-hour clock, and the zone ID. It always uses English, whatever the default locale
 *       of the computer is.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}.</li>
 * </ul>
 */
public final class MeetingTimes {

    private MeetingTimes() {
    }

    /** Returns the moment that is {@code localTime} in {@code fromZone}, as it looks in {@code toZone}. */
    public static ZonedDateTime convert(LocalDateTime localTime, ZoneId fromZone, ZoneId toZone) {
        // TODO: J07-01
        throw new UnsupportedOperationException("TODO: J07-01");
    }

    /** Formats {@code time} for an invitation, for example {@code "Sun 2026-03-08 16:30 (Asia/Seoul)"}. */
    public static String format(ZonedDateTime time) {
        // TODO: J07-01
        throw new UnsupportedOperationException("TODO: J07-01");
    }
}
