package com.example.bookstore.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Reference tests for S12-06: the security rules of the wallet API ({@link WalletService} lists them).
 *
 * <p>Three habits make security tests strong:
 * <ul>
 *   <li>Test every endpoint as each kind of caller: nobody (401), a member (their own data only) and an
 *       administrator. A rule that nobody tests is a rule that someone will break.</li>
 *   <li>Test both directions of a rule: the administrator <em>can</em> look up a wallet, and a member <em>can't</em>.
 *       A test that checks only one direction passes when the rule is missing or when it refuses everybody.</li>
 *   <li>Send what an attacker would send: parameters and JSON properties the API doesn't document, someone else's
 *       id, a negative amount. Then check the database, not only the status code.</li>
 * </ul>
 *
 * <p>Seed facts used here: Minji (member 1) has 100,000 won, the administrator (member 20) has 0.
 */
@IntegrationTest
@DisplayName("S12-06 · Write tests: security rules")
class WalletSecurityTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("every wallet endpoint answers 401 without a token")
        void everyEndpointNeedsAToken() {
            assertThat(mvc.get().uri("/api/wallet")).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(mvc.get().uri("/api/wallets/1")).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(transferWithoutToken("""
                    { "toMemberId": 20, "amount": 1000 }""")).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(balanceOf(TestUsers.MINJI)).isEqualTo(100_000);
        }

        @Test
        @DisplayName("a member sees their own wallet")
        void memberSeesOwnWallet() {
            assertThat(mvc.get().uri("/api/wallet").with(TestUsers.minji()))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "memberId": 1, "balance": 100000 }""");
        }

        @Test
        @DisplayName("a transfer moves money from the signed-in member to the recipient")
        void transferMovesMoney() {
            assertThat(transferAsMinji("""
                    { "toMemberId": 20, "amount": 10000 }"""))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "memberId": 1, "balance": 90000 }""");
            assertThat(balanceOf(TestUsers.MINJI)).isEqualTo(90_000);
            assertThat(balanceOf(TestUsers.ADMIN)).isEqualTo(10_000);
        }

        @Test
        @DisplayName("an administrator looks up any member's wallet")
        void adminLooksUpAnyWallet() {
            assertThat(mvc.get().uri("/api/wallets/1").with(TestUsers.admin()))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "memberId": 1, "balance": 100000 }""");
        }

        @Test
        @DisplayName("a member who isn't an administrator can't look up wallets: 403, also their own")
        void memberCannotLookUpWallets() {
            assertThat(mvc.get().uri("/api/wallets/20").with(TestUsers.minji()))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
            assertThat(mvc.get().uri("/api/wallets/1").with(TestUsers.minji()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("GET /api/wallet ignores a memberId parameter: it's always the signed-in member's wallet")
        void walletIgnoresAMemberIdParameter() {
            assertThat(mvc.get().uri("/api/wallet?memberId=20").with(TestUsers.minji()))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "memberId": 1, "balance": 100000 }""");
        }

        @Test
        @DisplayName("the money leaves the signed-in member's wallet, even when the body names another sender")
        void senderAlwaysComesFromTheToken() {
            assertThat(transferAsMinji("""
                    { "fromMemberId": 2, "toMemberId": 20, "amount": 10000 }""")).hasStatusOk();

            assertThat(balanceOf(TestUsers.MINJI)).isEqualTo(90_000);
            assertThat(balanceOf(TestUsers.ADMIN)).isEqualTo(10_000);
        }

        @Test
        @DisplayName("an administrator's own wallet works like everyone else's")
        void adminHasAnOrdinaryWallet() {
            assertThat(mvc.get().uri("/api/wallet").with(TestUsers.admin()))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "memberId": 20, "balance": 0 }""");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -10_000})
        @DisplayName("an amount that isn't positive is 400, and no money moves")
        void amountMustBePositive(long amount) {
            assertThat(transferAsMinji("""
                    { "toMemberId": 20, "amount": %d }""".formatted(amount)))
                    .hasStatus(HttpStatus.BAD_REQUEST);

            assertThat(balanceOf(TestUsers.MINJI)).isEqualTo(100_000);
            assertThat(balanceOf(TestUsers.ADMIN)).isZero();
        }

        @Test
        @DisplayName("more money than the wallet holds is 409, and no money moves")
        void cannotOverdraw() {
            assertThat(transferAsMinji("""
                    { "toMemberId": 20, "amount": 100001 }"""))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().extractingPath("$.code").isEqualTo("INSUFFICIENT_BALANCE");

            assertThat(balanceOf(TestUsers.MINJI)).isEqualTo(100_000);
        }

        @Test
        @DisplayName("sending money to yourself is 400")
        void cannotSendToSelf() {
            assertThat(transferAsMinji("""
                    { "toMemberId": 1, "amount": 1000 }""")).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("an unknown recipient is 404, and no money moves")
        void unknownRecipient() {
            assertThat(transferAsMinji("""
                    { "toMemberId": 9999, "amount": 1000 }"""))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("MEMBER_NOT_FOUND");

            assertThat(balanceOf(TestUsers.MINJI)).isEqualTo(100_000);
        }
    }

    private MvcTestResult transferAsMinji(String body) {
        return mvc.post().uri("/api/wallet/transfers").with(TestUsers.minji())
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private MvcTestResult transferWithoutToken(String body) {
        return mvc.post().uri("/api/wallet/transfers")
                .contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    /** Reads the balance from the database, after writing pending changes (SQL doesn't make Hibernate flush). */
    private long balanceOf(long memberId) {
        entityManager.flush();
        return jdbc.sql("SELECT balance FROM wallets WHERE member_id = :member").param("member", memberId)
                .query(Long.class).single();
    }
}
