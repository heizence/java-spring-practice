package practice.j07.ex01;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Set;

/**
 * Business-day arithmetic, like the {@code WORKDAY} function in spreadsheets. Use it for due dates such as "ships
 * within 3 business days".
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A business day is a Monday to Friday that is not in {@code holidays}.</li>
 *   <li>{@link #add} moves from {@code start} one day at a time until it has passed {@code days} business days, and
 *       returns the date where it stops. A negative {@code days} moves backward in the same way.</li>
 *   <li>{@code start} itself never counts, and it may be a weekend day or a holiday: Saturday plus 1 business day is
 *       Monday, and Saturday minus 1 business day is Friday.</li>
 *   <li>{@code days == 0} returns {@code start} unchanged, even when it isn't a business day.</li>
 *   <li>A holiday that falls on a weekend changes nothing.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}. Pass {@code Set.of()} when there are no
 *       holidays.</li>
 * </ul>
 */
public final class BusinessDays {

    private BusinessDays() {
    }

    /** Returns the date {@code days} business days after {@code start}, or before it if {@code days} is negative. */
    public static LocalDate add(LocalDate start, int days, Set<LocalDate> holidays) {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(holidays, "holidays");
        int step = Integer.signum(days); // +1 forward, -1 backward
        LocalDate date = start;
        // Count `remaining` toward 0 from either side instead of using Math.abs(days):
        // Math.abs(Integer.MIN_VALUE) is still negative.
        int remaining = days;
        while (remaining > 0) {
            date = date.plusDays(step); // move first, so the start date itself never counts
            if (isBusinessDay(date, holidays)) {
                remaining -= step;
            }
        }
        return date;
    }

    private static boolean isBusinessDay(LocalDate date, Set<LocalDate> holidays) {
        DayOfWeek day = date.getDayOfWeek(); // an enum: compare with ==, no magic numbers
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY && !holidays.contains(date);
    }
}
