package com.example.bookstore.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.stream.LongStream;

import com.example.bookstore.common.audit.AuditEntry;
import com.example.bookstore.common.audit.AuditEntry.Outcome;
import com.example.bookstore.common.audit.AuditLog;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.coupon.CampaignReport.SkippedMember;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The seed data used here (spring/BOOKSTORE.md): AUTUMN3000 was issued to members 1–10; FLASH10000 has 10 copies and
 * none were issued. Members 11 to 13 have neither. These tests need the audit log of S03-02.
 */
@IntegrationTest
@DisplayName("S03-03 · The self-invocation trap")
class CouponCampaignServiceTest {

    private static final Instant NOW = TestClock.DEFAULT_INSTANT;

    @Autowired
    private CouponCampaignService campaigns;

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private AuditLog auditLog;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("writes a CouponService.issue entry for each member of a campaign")
        void auditsEachMember() {
            campaigns.issueToAll("AUTUMN3000", List.of(11L, 12L, 13L));

            assertThat(issueEntries()).containsExactly(
                    succeeded("AUTUMN3000, 11"), succeeded("AUTUMN3000, 12"), succeeded("AUTUMN3000, 13"));
        }

        @Test
        @DisplayName("gives the coupon to every member of the campaign")
        void issuesTheCoupon() {
            CampaignReport report = campaigns.issueToAll("AUTUMN3000", List.of(11L, 12L, 13L));

            assertThat(report).isEqualTo(new CampaignReport("AUTUMN3000", List.of(11L, 12L, 13L), List.of()));
            assertThat(couponRepository.existsIssue("AUTUMN3000", 12)).isTrue();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("audits a member who already has the coupon as FAILED, and goes on with the next member")
        void auditsAndSkipsAMemberWhoHasTheCoupon() {
            CampaignReport report = campaigns.issueToAll("AUTUMN3000", List.of(11L, 1L, 12L));

            assertThat(issueEntries()).containsExactly(
                    succeeded("AUTUMN3000, 11"),
                    failed("AUTUMN3000, 1", ErrorCode.COUPON_ALREADY_ISSUED),
                    succeeded("AUTUMN3000, 12"));
            assertThat(report.issued()).containsExactly(11L, 12L);
            assertThat(report.skipped()).containsExactly(new SkippedMember(1, ErrorCode.COUPON_ALREADY_ISSUED));
        }

        @Test
        @DisplayName("audits the members after the coupon sold out as FAILED")
        void auditsMembersAfterTheCouponSoldOut() {
            List<Long> twelveMembers = LongStream.rangeClosed(1, 12).boxed().toList();

            CampaignReport report = campaigns.issueToAll("FLASH10000", twelveMembers);

            assertThat(issueEntries()).hasSize(12);
            assertThat(issueEntries().subList(0, 10)).extracting(AuditEntry::outcome).containsOnly(Outcome.SUCCEEDED);
            assertThat(issueEntries().subList(10, 12)).containsExactly(
                    failed("FLASH10000, 11", ErrorCode.COUPON_SOLD_OUT),
                    failed("FLASH10000, 12", ErrorCode.COUPON_SOLD_OUT));
            assertThat(report.skipped()).containsExactly(
                    new SkippedMember(11, ErrorCode.COUPON_SOLD_OUT), new SkippedMember(12, ErrorCode.COUPON_SOLD_OUT));
        }

        @Test
        @DisplayName("issues to and audits a member listed twice only once")
        void handlesADuplicateOnce() {
            CampaignReport report = campaigns.issueToAll("AUTUMN3000", List.of(11L, 11L));

            assertThat(issueEntries()).containsExactly(succeeded("AUTUMN3000, 11"));
            assertThat(report.issued()).containsExactly(11L);
            assertThat(report.skipped()).isEmpty();
        }

        @Test
        @DisplayName("writes the campaign's own entry after the members' entries")
        void auditsTheCampaignLast() {
            campaigns.issueToAll("AUTUMN3000", List.of(11L, 12L));

            assertThat(auditLog.entries()).last().isEqualTo(new AuditEntry(NOW, "CouponCampaignService.issueToAll",
                    "AUTUMN3000, [11, 12]", Outcome.SUCCEEDED, null));
        }

        @Test
        @DisplayName("still writes exactly one entry for a single issue")
        void auditsASingleIssueOnce() {
            couponService.issue("AUTUMN3000", 11);

            assertThat(auditLog.entries()).containsExactly(succeeded("AUTUMN3000, 11"));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an unknown coupon code stops the campaign with COUPON_NOT_FOUND before anyone gets anything")
        void rejectsAnUnknownCoupon() {
            assertThatThrownBy(() -> campaigns.issueToAll("SPRING9000", List.of(11L)))
                    .isInstanceOfSatisfying(BusinessException.class,
                            exception -> assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.COUPON_NOT_FOUND));
            assertThat(issueEntries()).isEmpty();
        }

        @Test
        @DisplayName("rejects a campaign with more than 1,000 members, and issues nothing")
        void rejectsTooManyMembers() {
            List<Long> members = LongStream.rangeClosed(11, 1_011).boxed().toList();

            assertThatThrownBy(() -> campaigns.issueToAll("AUTUMN3000", members))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1000");
            assertThat(couponRepository.existsIssue("AUTUMN3000", 11)).isFalse();
        }
    }

    /** The audit entries of single issues, oldest first. */
    private List<AuditEntry> issueEntries() {
        return auditLog.entries().stream()
                .filter(entry -> entry.action().equals("CouponService.issue"))
                .toList();
    }

    private static AuditEntry succeeded(String arguments) {
        return new AuditEntry(NOW, "CouponService.issue", arguments, Outcome.SUCCEEDED, null);
    }

    private static AuditEntry failed(String arguments, ErrorCode error) {
        return new AuditEntry(NOW, "CouponService.issue", arguments, Outcome.FAILED, error.name());
    }
}
