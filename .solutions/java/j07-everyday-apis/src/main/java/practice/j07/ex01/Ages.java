package practice.j07.ex01;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

/**
 * Ages in completed years, as forms and ID checks use them.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #ageOn} returns the number of full years from {@code birthDate} to {@code date}. On the birthday itself
 *       the new year counts: someone born on 2000-05-10 is 25 on 2026-05-09 and 26 on 2026-05-10.</li>
 *   <li>Someone born on 29 February gets one year older on 1 March in years without a 29 February. (That's how
 *       {@link java.time.Period#between} counts. Laws differ between countries; some use 28 February.)</li>
 *   <li>On the day of birth, the age is 0. A {@code date} before {@code birthDate} throws
 *       {@link IllegalArgumentException} with both dates in the message.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}.</li>
 * </ul>
 */
public final class Ages {

    private Ages() {
    }

    /** Returns the age, in full years, of someone born on {@code birthDate}, on {@code date}. */
    public static int ageOn(LocalDate birthDate, LocalDate date) {
        Objects.requireNonNull(birthDate, "birthDate");
        Objects.requireNonNull(date, "date");
        if (date.isBefore(birthDate)) {
            throw new IllegalArgumentException("date " + date + " is before the birth date " + birthDate);
        }
        // Period.between counts years, months and days the way people count birthdays, leap years included.
        // Hand-written versions that compare getDayOfYear() are off by one day after February in leap years.
        return Period.between(birthDate, date).getYears();
    }
}
