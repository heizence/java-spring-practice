package com.example.bookstore.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Template · Coupon and CouponIssue")
class CouponTest {

    private static final Instant VALID_FROM = Instant.parse("2026-10-01T00:00:00Z");
    private static final Instant VALID_UNTIL = Instant.parse("2026-10-31T23:59:59Z");
    private static final Instant IN_OCTOBER = Instant.parse("2026-10-15T00:00:00Z");

    private final Member member = Member.register("minji@example.com", "Minji Kim", "{noop}secret");

    @Nested
    @DisplayName("issue")
    class Issue {

        @Test
        @DisplayName("gives a copy to the member and counts it")
        void issuesACopy() {
            Coupon coupon = flashCoupon(10);

            CouponIssue issue = coupon.issue(member, IN_OCTOBER);

            assertThat(issue.getCoupon()).isSameAs(coupon);
            assertThat(issue.getMember()).isSameAs(member);
            assertThat(issue.getIssuedAt()).isEqualTo(IN_OCTOBER);
            assertThat(issue.isUsed()).isFalse();
            assertThat(coupon.getIssuedQuantity()).isEqualTo(1);
            assertThat(coupon.getRemainingQuantity()).isEqualTo(9);
        }

        @Test
        @DisplayName("works on the first and the last moment of the validity period")
        void issuesAtBothEndsOfThePeriod() {
            Coupon coupon = flashCoupon(10);

            coupon.issue(member, VALID_FROM);
            coupon.issue(member, VALID_UNTIL);

            assertThat(coupon.getIssuedQuantity()).isEqualTo(2);
        }

        @Test
        @DisplayName("rejects the request with COUPON_NOT_YET_VALID before the period")
        void rejectsBeforeThePeriod() {
            Coupon coupon = flashCoupon(10);

            assertThatThrownBy(() -> coupon.issue(member, VALID_FROM.minusNanos(1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Coupon FLASH10000 is valid from 2026-10-01T00:00:00Z.")
                    .extracting("errorCode").isEqualTo(ErrorCode.COUPON_NOT_YET_VALID);
            assertThat(coupon.getIssuedQuantity()).isZero();
        }

        @Test
        @DisplayName("rejects the request with COUPON_EXPIRED after the period")
        void rejectsAfterThePeriod() {
            Coupon coupon = flashCoupon(10);

            assertThatThrownBy(() -> coupon.issue(member, VALID_UNTIL.plusNanos(1)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Coupon FLASH10000 expired at 2026-10-31T23:59:59Z.")
                    .extracting("errorCode").isEqualTo(ErrorCode.COUPON_EXPIRED);
            assertThat(coupon.getIssuedQuantity()).isZero();
        }

        @Test
        @DisplayName("rejects the request with COUPON_SOLD_OUT once every copy is issued")
        void rejectsWhenSoldOut() {
            Coupon coupon = flashCoupon(2);
            coupon.issue(member, IN_OCTOBER);
            coupon.issue(member, IN_OCTOBER);

            assertThatThrownBy(() -> coupon.issue(member, IN_OCTOBER))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("All 2 copies of coupon FLASH10000 have been issued.")
                    .extracting("errorCode").isEqualTo(ErrorCode.COUPON_SOLD_OUT);
            assertThat(coupon.getIssuedQuantity()).isEqualTo(2);
            assertThat(coupon.getRemainingQuantity()).isZero();
        }
    }

    @Nested
    @DisplayName("use")
    class Use {

        @Test
        @DisplayName("marks the issued coupon as used at the given time")
        void marksAsUsed() {
            CouponIssue issue = flashCoupon(10).issue(member, IN_OCTOBER);
            Instant later = IN_OCTOBER.plus(Duration.ofDays(1));

            issue.use(later);

            assertThat(issue.isUsed()).isTrue();
            assertThat(issue.getUsedAt()).contains(later);
        }

        @Test
        @DisplayName("rejects a second use with COUPON_ALREADY_USED, and keeps the first time")
        void rejectsASecondUse() {
            CouponIssue issue = flashCoupon(10).issue(member, IN_OCTOBER);
            issue.use(IN_OCTOBER);

            assertThatThrownBy(() -> issue.use(IN_OCTOBER.plusSeconds(60)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COUPON_ALREADY_USED);
            assertThat(issue.getUsedAt()).contains(IN_OCTOBER);
        }

        @Test
        @DisplayName("rejects a use after the coupon expired with COUPON_EXPIRED")
        void rejectsAnExpiredCoupon() {
            CouponIssue issue = flashCoupon(10).issue(member, IN_OCTOBER);

            assertThatThrownBy(() -> issue.use(VALID_UNTIL.plusSeconds(1)))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.COUPON_EXPIRED);
            assertThat(issue.isUsed()).isFalse();
            assertThat(issue.getUsedAt()).isEmpty();
        }
    }

    @Nested
    @DisplayName("create")
    class Create {

        @Test
        @DisplayName("rejects a period that ends before it starts")
        void rejectsAnInvertedPeriod() {
            assertThatThrownBy(() -> Coupon.create("BROKEN", 1_000, 10, VALID_UNTIL, VALID_FROM))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a quantity or a discount below 1")
        void rejectsNonPositiveNumbers() {
            assertThatThrownBy(() -> Coupon.create("BROKEN", 1_000, 0, VALID_FROM, VALID_UNTIL))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Coupon.create("BROKEN", 0, 10, VALID_FROM, VALID_UNTIL))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    private static Coupon flashCoupon(int totalQuantity) {
        return Coupon.create("FLASH10000", 10_000, totalQuantity, VALID_FROM, VALID_UNTIL);
    }
}
