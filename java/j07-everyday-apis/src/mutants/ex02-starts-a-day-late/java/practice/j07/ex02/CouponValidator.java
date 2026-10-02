package practice.j07.ex02;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Decides whether a coupon can be used right now.
 *
 * <p><strong>This class is finished and correct.</strong> In exercise J07-02 you write the tests for it.
 *
 * <h2>Rules</h2>
 * <ol>
 *   <li>"Now" always comes from the {@link Clock} passed to the constructor, never from the computer's clock.</li>
 *   <li>Coupon dates are dates in Korea. "Today" is the current date in {@code Asia/Seoul}, whatever time zone the
 *       clock uses. (Servers usually run in UTC, which is 9 hours behind Seoul.)</li>
 *   <li>Both dates of a coupon are inclusive. It's {@link CouponStatus#NOT_YET_VALID} before {@code validFrom}, valid
 *       from 00:00 Seoul time on {@code validFrom} until the end of {@code validUntil}, and
 *       {@link CouponStatus#EXPIRED} from 00:00 Seoul time on the day after {@code validUntil}.</li>
 *   <li>A {@code weekendsOnly} coupon is {@link CouponStatus#WEEKENDS_ONLY} from Monday to Friday, Seoul time. It
 *       can be used on Saturday and Sunday.</li>
 *   <li>The checks run in the order above: a coupon that is expired is {@link CouponStatus#EXPIRED} on a weekday,
 *       too. When no check fails, the status is {@link CouponStatus#VALID}.</li>
 *   <li>A {@code null} clock or coupon throws {@link NullPointerException}.</li>
 * </ol>
 */
public final class CouponValidator {

    /** The time zone of coupon dates. */
    public static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    private final Clock clock;

    public CouponValidator(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Returns whether {@code coupon} can be used now, and if not, why. */
    public CouponStatus check(Coupon coupon) {
        Objects.requireNonNull(coupon, "coupon");
        // Read the clock once, and turn the moment into a date in Seoul. LocalDate.now(clock) would use the clock's
        // own zone instead, which is UTC on most servers.
        LocalDate today = LocalDate.ofInstant(clock.instant(), SEOUL);

        if (!today.isAfter(coupon.validFrom())) {
            return CouponStatus.NOT_YET_VALID;
        }
        if (today.isAfter(coupon.validUntil())) {
            return CouponStatus.EXPIRED;
        }
        if (coupon.weekendsOnly() && !isWeekend(today.getDayOfWeek())) {
            return CouponStatus.WEEKENDS_ONLY;
        }
        return CouponStatus.VALID;
    }

    private static boolean isWeekend(DayOfWeek day) {
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }
}
