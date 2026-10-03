package com.example.bookstore.common.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;

import com.example.bookstore.book.BookService;
import com.example.bookstore.common.audit.AuditEntry.Outcome;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.coupon.CouponIssue;
import com.example.bookstore.coupon.CouponRepository;
import com.example.bookstore.coupon.CouponService;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The seed data used here (spring/BOOKSTORE.md): AUTUMN3000 is valid and was issued to members 1–10, EXPIRED2000
 * expired in August 2026. Member 11 has neither.
 */
@IntegrationTest
@DisplayName("S03-02 · Pointcuts and advice types")
class AuditAspectTest {

    private static final Instant NOW = TestClock.DEFAULT_INSTANT;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private BookService bookService;

    @Autowired
    private AuditLog auditLog;

    @Autowired
    private TestClock clock;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("writes a SUCCEEDED entry for a coupon service call that returns normally")
        void auditsASuccess() {
            couponService.issue("AUTUMN3000", 11);

            assertThat(auditLog.entries()).containsExactly(
                    new AuditEntry(NOW, "CouponService.issue", "AUTUMN3000, 11", Outcome.SUCCEEDED, null));
        }

        @Test
        @DisplayName("writes a FAILED entry with the error code for a call that throws a BusinessException")
        void auditsAFailure() {
            catchThrowable(() -> couponService.issue("EXPIRED2000", 11));

            assertThat(auditLog.entries()).containsExactly(
                    new AuditEntry(NOW, "CouponService.issue", "EXPIRED2000, 11", Outcome.FAILED, "COUPON_EXPIRED"));
        }

        @Test
        @DisplayName("returns the method's result to the caller")
        void returnsTheResult() {
            CouponIssue issue = couponService.issue("AUTUMN3000", 11);

            assertThat(issue).isEqualTo(new CouponIssue("AUTUMN3000", 11, NOW));
        }

        @Test
        @DisplayName("lets the exception reach the caller")
        void rethrowsTheException() {
            assertThatThrownBy(() -> couponService.issue("EXPIRED2000", 11))
                    .isInstanceOfSatisfying(BusinessException.class,
                            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_EXPIRED));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("doesn't audit find… methods, which only read")
        void ignoresFindMethods() {
            couponService.findCoupon("WELCOME5000");

            assertThat(auditLog.entries()).isEmpty();
        }

        @Test
        @DisplayName("doesn't audit the coupon repository: it's a @Repository, not a @Service")
        void ignoresTheRepository() {
            couponRepository.findByCode("WELCOME5000");
            couponRepository.existsIssue("WELCOME5000", 1);

            assertThat(auditLog.entries()).isEmpty();
        }

        @Test
        @DisplayName("doesn't audit services outside the coupon package")
        void ignoresOtherPackages() {
            bookService.search("java");

            assertThat(auditLog.entries()).isEmpty();
        }

        @Test
        @DisplayName("writes only the FAILED entry for a call that fails, no SUCCEEDED entry")
        void writesOneEntryForAFailure() {
            catchThrowable(() -> couponService.issue("AUTUMN3000", 1)); // member 1 already has AUTUMN3000

            assertThat(auditLog.entries()).singleElement()
                    .extracting(AuditEntry::outcome, AuditEntry::error)
                    .containsExactly(Outcome.FAILED, "COUPON_ALREADY_ISSUED");
        }

        @Test
        @DisplayName("audits every call, in the order the calls ended")
        void auditsEveryCall() {
            couponService.issue("AUTUMN3000", 11);
            catchThrowable(() -> couponService.issue("AUTUMN3000", 11));

            assertThat(auditLog.entries()).containsExactly(
                    new AuditEntry(NOW, "CouponService.issue", "AUTUMN3000, 11", Outcome.SUCCEEDED, null),
                    new AuditEntry(NOW, "CouponService.issue", "AUTUMN3000, 11", Outcome.FAILED,
                            "COUPON_ALREADY_ISSUED"));
        }

        @Test
        @DisplayName("takes the time of an entry from the application's clock")
        void usesTheClock() {
            Instant later = Instant.parse("2026-10-20T09:30:00Z");
            clock.setInstant(later);

            couponService.issue("AUTUMN3000", 11);

            assertThat(auditLog.entries()).extracting(AuditEntry::at).containsExactly(later);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("audits a failure that isn't a BusinessException with the exception's class name")
        void auditsOtherExceptions() {
            catchThrowable(() -> couponService.issue("WELCOME5000", 0)); // an invalid member id: a programming error

            assertThat(auditLog.entries()).containsExactly(
                    new AuditEntry(NOW, "CouponService.issue", "WELCOME5000, 0", Outcome.FAILED,
                            "IllegalArgumentException"));
        }

        @Test
        @DisplayName("lets an exception that isn't a BusinessException reach the caller too")
        void rethrowsOtherExceptions() {
            assertThatThrownBy(() -> couponService.issue("WELCOME5000", 0))
                    .isExactlyInstanceOf(IllegalArgumentException.class)
                    .hasMessage("memberId must be positive: 0");
        }
    }
}
