package practice.j07.ex02;

import java.time.LocalDate;
import java.util.Objects;

/**
 * A discount coupon, as the marketing team defines it.
 *
 * @param code         the code customers type in, for example {@code "SPRING25"}
 * @param validFrom    the first day the coupon can be used, as a date in Korea
 * @param validUntil   the last day the coupon can be used, as a date in Korea
 * @param weekendsOnly whether the coupon works only on Saturdays and Sundays
 */
public record Coupon(String code, LocalDate validFrom, LocalDate validUntil, boolean weekendsOnly) {

    /**
     * @throws NullPointerException     if {@code code}, {@code validFrom} or {@code validUntil} is {@code null}
     * @throws IllegalArgumentException if {@code validUntil} is before {@code validFrom}
     */
    public Coupon {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(validFrom, "validFrom");
        Objects.requireNonNull(validUntil, "validUntil");
        if (validUntil.isBefore(validFrom)) {
            throw new IllegalArgumentException(
                    "coupon " + code + " ends on " + validUntil + ", before it starts on " + validFrom);
        }
    }
}
