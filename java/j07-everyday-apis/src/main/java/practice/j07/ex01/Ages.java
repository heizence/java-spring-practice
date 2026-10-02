package practice.j07.ex01;

import java.time.LocalDate;

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
        // TODO: J07-01
        throw new UnsupportedOperationException("TODO: J07-01");
    }
}
