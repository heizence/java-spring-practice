package practice.j07.ex02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Reference tests for J07-02.
 *
 * <p>Three habits make these tests strong:
 * <ul>
 *   <li>Every validator gets a stopped clock ({@link Clock#fixed}), so every test sees the same "now", today and in
 *       ten years. The coupon dates are in the past, so a validator that secretly reads the real clock fails.</li>
 *   <li>The clock runs in UTC, like most servers. A test clock in {@code Asia/Seoul} would hide every bug that mixes
 *       up the two zones.</li>
 *   <li>Each boundary is tested on both sides, to the second: the last moment before a coupon starts and the first
 *       moment it's valid, the last moment of its last day and the first moment after it.</li>
 * </ul>
 */
@DisplayName("J07-02 · Write tests: time-dependent code")
class CouponValidatorTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    /** Valid for the whole of March 2025. 1 March 2025 is a Saturday. */
    private static final Coupon MARCH =
            new Coupon("SPRING25", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31), false);
    private static final Coupon MARCH_WEEKENDS =
            new Coupon("WEEKEND25", LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31), true);

    /** A validator whose clock is stopped at this wall-clock time in Seoul. The clock itself runs in UTC. */
    private static CouponValidator at(String seoulDateTime) {
        Instant now = LocalDateTime.parse(seoulDateTime).atZone(SEOUL).toInstant();
        return new CouponValidator(Clock.fixed(now, ZoneOffset.UTC));
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("is valid in the middle of its period")
        void validInTheMiddle() {
            assertThat(at("2025-03-12T12:00").check(MARCH)).isEqualTo(CouponStatus.VALID);
        }

        @Test
        @DisplayName("is not valid yet before its first day")
        void notYetValid() {
            assertThat(at("2025-02-20T12:00").check(MARCH)).isEqualTo(CouponStatus.NOT_YET_VALID);
        }

        @Test
        @DisplayName("is expired after its last day")
        void expired() {
            assertThat(at("2025-04-10T12:00").check(MARCH)).isEqualTo(CouponStatus.EXPIRED);
        }

        @Test
        @DisplayName("a weekends-only coupon works on a Saturday")
        void weekendCouponOnSaturday() {
            assertThat(at("2025-03-15T12:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.VALID);
        }

        @Test
        @DisplayName("a weekends-only coupon doesn't work on a Wednesday")
        void weekendCouponOnWednesday() {
            assertThat(at("2025-03-12T12:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.WEEKENDS_ONLY);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("is valid from 00:00 Seoul time on its first day")
        void validFromMidnightOnFirstDay() {
            assertThat(at("2025-03-01T00:00").check(MARCH)).isEqualTo(CouponStatus.VALID);
        }

        @Test
        @DisplayName("is not valid yet in the last second before its first day")
        void notValidJustBeforeFirstDay() {
            assertThat(at("2025-02-28T23:59:59").check(MARCH)).isEqualTo(CouponStatus.NOT_YET_VALID);
        }

        @Test
        @DisplayName("is still valid in the last moment of its last day")
        void validUntilEndOfLastDay() {
            assertThat(at("2025-03-31T23:59:59.999999999").check(MARCH)).isEqualTo(CouponStatus.VALID);
        }

        @Test
        @DisplayName("expires at 00:00 Seoul time after its last day")
        void expiresAtMidnightAfterLastDay() {
            assertThat(at("2025-04-01T00:00").check(MARCH)).isEqualTo(CouponStatus.EXPIRED);
        }

        @Test
        @DisplayName("uses the date in Seoul, not in UTC, before 09:00 Seoul time")
        void usesSeoulDateNotUtcDate() {
            // 08:59 in Seoul is 23:59 of the previous day in UTC.
            assertThat(at("2025-03-01T08:59").check(MARCH)).isEqualTo(CouponStatus.VALID);
            assertThat(at("2025-04-01T08:59").check(MARCH)).isEqualTo(CouponStatus.EXPIRED);
        }

        @Test
        @DisplayName("a weekends-only coupon works on a Sunday")
        void weekendCouponOnSunday() {
            assertThat(at("2025-03-16T12:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.VALID);
        }

        @Test
        @DisplayName("a weekends-only coupon doesn't work on the weekdays next to the weekend")
        void weekendCouponOnFridayAndMonday() {
            assertThat(at("2025-03-14T12:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.WEEKENDS_ONLY);
            assertThat(at("2025-03-17T12:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.WEEKENDS_ONLY);
        }

        @Test
        @DisplayName("uses the day of the week in Seoul, not in UTC")
        void usesSeoulDayOfWeek() {
            // Saturday 08:00 in Seoul is still Friday in UTC; Monday 08:00 in Seoul is still Sunday in UTC.
            assertThat(at("2025-03-15T08:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.VALID);
            assertThat(at("2025-03-17T08:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.WEEKENDS_ONLY);
        }

        @Test
        @DisplayName("checks the dates before the weekend rule")
        void datesBeforeWeekendRule() {
            assertThat(at("2025-04-05T12:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.EXPIRED); // a Saturday
            assertThat(at("2025-02-26T12:00").check(MARCH_WEEKENDS)).isEqualTo(CouponStatus.NOT_YET_VALID);
        }

        @Test
        @DisplayName("a one-day coupon is valid for the whole of that day")
        void oneDayCoupon() {
            var flashSale = new Coupon("FLASH", LocalDate.of(2025, 3, 20), LocalDate.of(2025, 3, 20), false);

            assertThat(at("2025-03-20T00:00").check(flashSale)).isEqualTo(CouponStatus.VALID);
            assertThat(at("2025-03-20T23:59:59").check(flashSale)).isEqualTo(CouponStatus.VALID);
            assertThat(at("2025-03-21T00:00").check(flashSale)).isEqualTo(CouponStatus.EXPIRED);
        }

        @Test
        @DisplayName("uses the clock it was given: two clocks, two answers")
        void usesGivenClock() {
            assertThat(at("2025-02-28T12:00").check(MARCH)).isEqualTo(CouponStatus.NOT_YET_VALID);
            assertThat(at("2025-03-28T12:00").check(MARCH)).isEqualTo(CouponStatus.VALID);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a null clock")
        void rejectsNullClock() {
            assertThatThrownBy(() -> new CouponValidator(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a null coupon")
        void rejectsNullCoupon() {
            assertThatThrownBy(() -> at("2025-03-12T12:00").check(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("a coupon can't end before it starts")
        void couponEndsBeforeItStarts() {
            assertThatThrownBy(() -> new Coupon("BROKEN", LocalDate.of(2025, 3, 31), LocalDate.of(2025, 3, 1), false))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
