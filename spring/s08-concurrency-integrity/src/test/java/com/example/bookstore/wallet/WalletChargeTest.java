package com.example.bookstore.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.Responses;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.SqlStatementCounter;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Charging a wallet: {@code POST /api/members/{memberId}/wallet/charges}. Minji (member 1) has 100,000 won
 * ({@code spring/BOOKSTORE.md}).
 *
 * <p>Every request commits, as in production, and each test puts the seed data back afterwards.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S08-02 · Optimistic locking")
class WalletChargeTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedData seedData;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a charge adds the amount and answers with the new balance")
        void addsTheAmount() {
            MvcTestResult result = charge(1, 10_000);

            assertThat(result)
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "memberId": 1, "balance": 110000 }""");
            assertThat(balanceOf(1)).isEqualTo(110_000);
        }

        @Test
        @DisplayName("a charge is recorded as a CHARGE in the wallet's history, at the current time")
        void recordsTheCharge() {
            long chargesBefore = chargesOf(1);

            charge(1, 10_000);

            assertThat(chargesOf(1)).isEqualTo(chargesBefore + 1);
            assertThat(jdbc.sql("""
                    SELECT t.amount, t.created_at FROM wallet_transactions t JOIN wallets w ON w.id = t.wallet_id
                    WHERE w.member_id = 1 ORDER BY t.id DESC LIMIT 1""")
                    .query((row, rowNumber) -> row.getLong("amount") + " at "
                            + row.getTimestamp("created_at").toInstant())
                    .single())
                    .isEqualTo("10000 at " + TestClock.DEFAULT_INSTANT);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("charges one after another add up, and each one increases the wallet's version")
        void chargesAddUp() {
            long versionBefore = versionOf(1);

            charge(1, 1_000);
            MvcTestResult second = charge(1, 2_500);

            assertThat(second).bodyJson().extractingPath("$.balance").asNumber().isEqualTo(103_500);
            assertThat(balanceOf(1)).isEqualTo(103_500);
            assertThat(versionOf(1)).isEqualTo(versionBefore + 2);
        }

        @Test
        @DisplayName("an empty wallet can be charged: the admin starts with 0 won")
        void chargesAnEmptyWallet() {
            MvcTestResult result = charge(20, 1);

            assertThat(result).hasStatusOk();
            assertThat(balanceOf(20)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("charging the wallet of a member who doesn't exist fails with 404 MEMBER_NOT_FOUND")
        void unknownMember() {
            MvcTestResult result = charge(9_999, 10_000);

            assertThat(result)
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("MEMBER_NOT_FOUND");
        }

        @Test
        @DisplayName("a failure that another attempt can't fix, such as an unknown member, is not tried again")
        void doesNotRetryBusinessErrors() {
            charge(9_999, 10_000);

            List<String> walletQueries = sqlStatements.statements().stream()
                    .filter(sql -> sql.contains("from wallets"))
                    .toList();
            assertThat(walletQueries).as("queries for the wallet").hasSize(1);
        }

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -1, 1_000_001})
        @DisplayName("an amount outside 1 to 1,000,000 won is invalid input, and changes nothing")
        void invalidAmount(long amount) {
            MvcTestResult result = charge(1, amount);

            assertThat(result)
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT");
            assertThat(balanceOf(1)).isEqualTo(100_000);
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("10 charges sent at the same moment all succeed, and each is applied exactly once")
        void appliesEveryChargeOnce() {
            long chargesBefore = chargesOf(1);

            Responses responses = Responses.sendAtOnce(10, i -> charge(1, 1_000));

            assertThat(responses.countByOutcome()).isEqualTo(Map.of("200", 10L));
            assertThat(balanceOf(1)).isEqualTo(110_000);
            assertThat(chargesOf(1) - chargesBefore).as("new CHARGE transactions").isEqualTo(10);
        }

        @Test
        @DisplayName("4 clients that each send 5 charges in a row, at the same time, get all 20 charges applied once")
        void appliesChargesFromSeveralClients() {
            long chargesBefore = chargesOf(1);

            Responses responses = Responses.sendAtOnce(4, client -> {
                MvcTestResult last = null;
                for (int i = 0; i < 5; i++) {
                    last = charge(1, 100);
                    if (last.getResponse().getStatus() != 200) {
                        return last;
                    }
                }
                return last;
            });

            assertThat(responses.countByOutcome()).isEqualTo(Map.of("200", 4L));
            assertThat(balanceOf(1)).isEqualTo(102_000);
            assertThat(chargesOf(1) - chargesBefore).as("new CHARGE transactions").isEqualTo(20);
        }
    }

    private MvcTestResult charge(long memberId, long amount) {
        return mvc.post().uri("/api/members/{memberId}/wallet/charges", memberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "amount": %d }""".formatted(amount))
                .exchange();
    }

    private long balanceOf(long memberId) {
        return jdbc.sql("SELECT balance FROM wallets WHERE member_id = ?").param(memberId).query(Long.class).single();
    }

    private long versionOf(long memberId) {
        return jdbc.sql("SELECT version FROM wallets WHERE member_id = ?").param(memberId).query(Long.class).single();
    }

    private long chargesOf(long memberId) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM wallet_transactions t JOIN wallets w ON w.id = t.wallet_id
                WHERE w.member_id = ? AND t.type = 'CHARGE'""")
                .param(memberId).query(Long.class).single();
    }
}
