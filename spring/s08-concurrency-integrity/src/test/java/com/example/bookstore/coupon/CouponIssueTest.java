package com.example.bookstore.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.Responses;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.TestClock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Issuing coupons: {@code POST /api/coupons/{code}/issues}. The seed coupons ({@code spring/BOOKSTORE.md}):
 * {@code FLASH10000} has 10 copies, none issued yet, valid in October 2026; {@code WELCOME5000} was issued to members
 * 1–15; {@code EXPIRED2000} was valid in July and August 2026 and members 2–6 got it. The clock stands at
 * 2026-10-15T00:00:00Z.
 *
 * <p>Every request commits, as in production, and each test puts the seed data back afterwards.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S08-05 · First-come coupons")
class CouponIssueTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private TestClock clock;

    @Autowired
    private SeedData seedData;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a member gets a copy of the coupon: 201 with the issue, and the coupon counts it")
        void issuesACopy() {
            MvcTestResult result = issue("FLASH10000", 1);

            assertThat(result)
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson().isLenientlyEqualTo("""
                            {
                              "couponCode": "FLASH10000",
                              "memberId": 1,
                              "discountAmount": 10000,
                              "issuedAt": "%s"
                            }""".formatted(TestClock.DEFAULT_INSTANT));
            Number id = JsonPath.read(Responses.body(result), "$.id");
            assertThat(result).headers().hasValue(HttpHeaders.LOCATION, "/api/coupon-issues/" + id);
            assertThat(issuedQuantityOf("FLASH10000")).isEqualTo(1);
            assertThat(issuesOf("FLASH10000")).isEqualTo(1);
        }

        @Test
        @DisplayName("different members each get a copy")
        void issuesToSeveralMembers() {
            assertThat(issue("FLASH10000", 1)).hasStatus(HttpStatus.CREATED);
            assertThat(issue("FLASH10000", 2)).hasStatus(HttpStatus.CREATED);

            assertThat(issuedQuantityOf("FLASH10000")).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the last copy can be issued")
        void issuesTheLastCopy() {
            issueToMembers("FLASH10000", 1, 9);

            MvcTestResult tenth = issue("FLASH10000", 10);

            assertThat(tenth).hasStatus(HttpStatus.CREATED);
            assertThat(issuedQuantityOf("FLASH10000")).isEqualTo(10);
        }

        @Test
        @DisplayName("a coupon can be issued on the first and on the last moment of its validity period")
        void issuesAtTheEdgesOfThePeriod() {
            clock.setInstant(Instant.parse("2026-10-01T00:00:00Z"));
            assertThat(issue("FLASH10000", 1)).hasStatus(HttpStatus.CREATED);

            clock.setInstant(Instant.parse("2026-10-31T23:59:59Z"));
            assertThat(issue("FLASH10000", 2)).hasStatus(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("when every copy has been issued, the next request fails with 409 COUPON_SOLD_OUT")
        void soldOut() {
            issueToMembers("FLASH10000", 1, 10);

            MvcTestResult result = issue("FLASH10000", 11);

            assertThat(result)
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().extractingPath("$.code").isEqualTo("COUPON_SOLD_OUT");
            assertThat(issuedQuantityOf("FLASH10000")).isEqualTo(10);
            assertThat(issuesOf("FLASH10000")).isEqualTo(10);
        }

        @Test
        @DisplayName("a member who already has the coupon gets 409 COUPON_ALREADY_ISSUED, and nothing is counted")
        void alreadyIssued() {
            MvcTestResult result = issue("WELCOME5000", 1);

            assertThat(Responses.outcome(result)).isEqualTo("409 COUPON_ALREADY_ISSUED");
            assertThat(issuedQuantityOf("WELCOME5000")).isEqualTo(15);
            assertThat(issuesOf("WELCOME5000")).isEqualTo(15);
        }

        @Test
        @DisplayName("an expired coupon fails with 409 COUPON_EXPIRED")
        void expired() {
            assertThat(Responses.outcome(issue("EXPIRED2000", 1))).isEqualTo("409 COUPON_EXPIRED");
        }

        @Test
        @DisplayName("a coupon whose period hasn't started fails with 409 COUPON_NOT_YET_VALID")
        void notYetValid() {
            clock.setInstant(Instant.parse("2026-09-30T23:59:59Z"));

            assertThat(Responses.outcome(issue("FLASH10000", 1))).isEqualTo("409 COUPON_NOT_YET_VALID");
            assertThat(issuedQuantityOf("FLASH10000")).isZero();
        }

        @Test
        @DisplayName("an unknown coupon code fails with 404 COUPON_NOT_FOUND")
        void unknownCoupon() {
            assertThat(Responses.outcome(issue("NOPE1000", 1))).isEqualTo("404 COUPON_NOT_FOUND");
        }

        @Test
        @DisplayName("a member who doesn't exist fails with 404 MEMBER_NOT_FOUND, and nothing is counted")
        void unknownMember() {
            assertThat(Responses.outcome(issue("FLASH10000", 9_999))).isEqualTo("404 MEMBER_NOT_FOUND");
            assertThat(issuedQuantityOf("FLASH10000")).isZero();
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("100 members who ask for FLASH10000 at the same moment get exactly 10 copies; "
                + "the others get 409 COUPON_SOLD_OUT")
        void issuesExactlyTheCopiesThereAre() {
            List<Long> members = newMembers(100);

            Responses responses = Responses.sendAtOnce(100, i -> issue("FLASH10000", members.get(i)));

            assertThat(responses.countByOutcome()).isEqualTo(Map.of("201", 10L, "409 COUPON_SOLD_OUT", 90L));
            assertThat(issuedQuantityOf("FLASH10000")).isEqualTo(10);
            assertThat(issuesOf("FLASH10000")).isEqualTo(10);
        }

        @Test
        @DisplayName("a member who asks 10 times at the same moment gets one copy; "
                + "the other requests get 409 COUPON_ALREADY_ISSUED")
        void issuesOneCopyPerMember() {
            Responses responses = Responses.sendAtOnce(10, i -> issue("FLASH10000", 1));

            assertThat(responses.countByOutcome()).isEqualTo(Map.of("201", 1L, "409 COUPON_ALREADY_ISSUED", 9L));
            assertThat(issuedQuantityOf("FLASH10000")).isEqualTo(1);
            assertThat(issuesOf("FLASH10000")).isEqualTo(1);
        }
    }

    private MvcTestResult issue(String code, long memberId) {
        return mvc.post().uri("/api/coupons/{code}/issues", code)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "memberId": %d }""".formatted(memberId))
                .exchange();
    }

    /** Issues the coupon to the members from {@code first} to {@code last}, one after another. */
    private void issueToMembers(String code, long first, long last) {
        for (long member = first; member <= last; member++) {
            assertThat(issue(code, member)).as("issue to member %d", member).hasStatus(HttpStatus.CREATED);
        }
    }

    /** Signs up new members, and returns their ids. */
    private List<Long> newMembers(int count) {
        List<Member> members = IntStream.range(0, count)
                .mapToObj(i -> Member.register("flash" + i + "@example.com", "Flash Buyer " + i, "{noop}secret"))
                .toList();
        return memberRepository.saveAll(members).stream().map(Member::getId).toList();
    }

    private int issuedQuantityOf(String code) {
        return jdbc.sql("SELECT issued_quantity FROM coupons WHERE code = ?").param(code)
                .query(Integer.class).single();
    }

    private long issuesOf(String code) {
        return jdbc.sql("SELECT COUNT(*) FROM coupon_issues i JOIN coupons c ON c.id = i.coupon_id WHERE c.code = ?")
                .param(code).query(Long.class).single();
    }
}
