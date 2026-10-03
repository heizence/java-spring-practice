package com.example.bookstore.wallet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.Responses;
import com.example.bookstore.support.SeedData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import practice.testkit.Concurrently;

/**
 * Transfers between wallets: {@code POST /api/members/{memberId}/wallet/transfers}. Minji (member 1) has 100,000
 * won and the admin (member 20) has 0 ({@code spring/BOOKSTORE.md}); the tests read every other balance first.
 *
 * <p>Every request commits, as in production, and each test puts the seed data back afterwards.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S08-04 · Deadlocks")
class WalletTransferTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

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
        @DisplayName("a transfer moves the amount and answers with the sender's new balance")
        void movesTheAmount() {
            long junBefore = balanceOf(2);

            MvcTestResult result = transfer(1, 2, 30_000);

            assertThat(result)
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "memberId": 1, "balance": 70000 }""");
            assertThat(balanceOf(1)).isEqualTo(70_000);
            assertThat(balanceOf(2)).isEqualTo(junBefore + 30_000);
        }

        @Test
        @DisplayName("a transfer is recorded as TRANSFER_OUT for the sender and TRANSFER_IN for the receiver")
        void recordsBothSides() {
            transfer(1, 2, 30_000);

            assertThat(lastTransactionOf(1)).isEqualTo("TRANSFER_OUT 30000");
            assertThat(lastTransactionOf(2)).isEqualTo("TRANSFER_IN 30000");
        }

        @Test
        @DisplayName("a transfer to a member with a lower id works the same way")
        void transfersToALowerId() {
            long junBefore = balanceOf(2);

            MvcTestResult result = transfer(2, 1, 1_000);

            assertThat(result).hasStatusOk();
            assertThat(balanceOf(2)).isEqualTo(junBefore - 1_000);
            assertThat(balanceOf(1)).isEqualTo(101_000);
            assertThat(lastTransactionOf(2)).isEqualTo("TRANSFER_OUT 1000");
            assertThat(lastTransactionOf(1)).isEqualTo("TRANSFER_IN 1000");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a member can send the whole balance")
        void sendsTheWholeBalance() {
            MvcTestResult result = transfer(1, 20, 100_000);

            assertThat(result).hasStatusOk();
            assertThat(balanceOf(1)).isZero();
            assertThat(balanceOf(20)).isEqualTo(100_000);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a transfer of more than the balance fails with 409 INSUFFICIENT_BALANCE, and changes nothing")
        void insufficientBalance() {
            long junBefore = balanceOf(2);

            MvcTestResult result = transfer(20, 2, 1);

            assertThat(Responses.outcome(result)).isEqualTo("409 INSUFFICIENT_BALANCE");
            assertThat(balanceOf(20)).isZero();
            assertThat(balanceOf(2)).isEqualTo(junBefore);
        }

        @Test
        @DisplayName("a transfer to a member who doesn't exist fails with 404 MEMBER_NOT_FOUND, and changes nothing")
        void unknownReceiver() {
            MvcTestResult result = transfer(1, 9_999, 1_000);

            assertThat(Responses.outcome(result)).isEqualTo("404 MEMBER_NOT_FOUND");
            assertThat(balanceOf(1)).isEqualTo(100_000);
        }

        @Test
        @DisplayName("a transfer from a member who doesn't exist fails with 404 MEMBER_NOT_FOUND")
        void unknownSender() {
            assertThat(Responses.outcome(transfer(9_999, 1, 1_000))).isEqualTo("404 MEMBER_NOT_FOUND");
        }

        @Test
        @DisplayName("a transfer to oneself fails with 400 INVALID_INPUT")
        void toOneself() {
            MvcTestResult result = transfer(1, 1, 1_000);

            assertThat(result)
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.detail").isEqualTo("A member can't send money to themselves.");
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("two members who send each other money at the same moment, 30 times in a row, never deadlock")
        void oppositeTransfersDoNotDeadlock() {
            transfer(1, 2, 10_000); // so that both have money to send
            long minjiBefore = balanceOf(1);
            long junBefore = balanceOf(2);
            var round = new CyclicBarrier(2);
            Queue<MvcTestResult> results = new ConcurrentLinkedQueue<>();

            // Thread 0 sends from Minji to Jun, thread 1 from Jun to Minji. Both wait for each other before every
            // transfer, so that every round starts two opposite transfers at the same moment.
            Concurrently.run(2, thread -> {
                for (int i = 0; i < 30; i++) {
                    await(round);
                    results.add(thread == 0 ? transfer(1, 2, 100) : transfer(2, 1, 100));
                }
            });

            assertThat(countByOutcome(results)).isEqualTo(Map.of("200", 60L));
            assertThat(balanceOf(1)).isEqualTo(minjiBefore);
            assertThat(balanceOf(2)).isEqualTo(junBefore);
        }

        @Test
        @DisplayName("random transfers among four members on 8 threads never collide, and keep the total")
        void randomTransfersKeepTheTotal() {
            List<Long> members = List.of(1L, 2L, 3L, 4L);
            long totalBefore = members.stream().mapToLong(WalletTransferTest.this::balanceOf).sum();
            Queue<MvcTestResult> results = new ConcurrentLinkedQueue<>();

            Concurrently.run(8, thread -> {
                var random = new Random(thread); // a fixed seed per thread: the same transfers in every run
                for (int i = 0; i < 15; i++) {
                    long from = members.get(random.nextInt(4));
                    long to = members.get(random.nextInt(4));
                    if (from != to) {
                        results.add(transfer(from, to, 1 + random.nextInt(1_000)));
                    }
                }
            });

            // A member may run out of money now and then; that is a normal answer. A collision isn't.
            assertThat(countByOutcome(results).keySet()).isSubsetOf("200", "409 INSUFFICIENT_BALANCE");
            assertThat(members.stream().mapToLong(WalletTransferTest.this::balanceOf).sum())
                    .as("the members' total balance").isEqualTo(totalBefore);
        }
    }

    private MvcTestResult transfer(long fromMemberId, long toMemberId, long amount) {
        return mvc.post().uri("/api/members/{memberId}/wallet/transfers", fromMemberId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "toMemberId": %d, "amount": %d }""".formatted(toMemberId, amount))
                .exchange();
    }

    private long balanceOf(long memberId) {
        return jdbc.sql("SELECT balance FROM wallets WHERE member_id = ?").param(memberId).query(Long.class).single();
    }

    /** The type and amount of the member's newest wallet transaction, such as {@code "TRANSFER_IN 1000"}. */
    private String lastTransactionOf(long memberId) {
        return jdbc.sql("""
                SELECT CONCAT(t.type, ' ', t.amount) FROM wallet_transactions t JOIN wallets w ON w.id = t.wallet_id
                WHERE w.member_id = ? ORDER BY t.id DESC LIMIT 1""")
                .param(memberId).query(String.class).single();
    }

    private static Map<String, Long> countByOutcome(Queue<MvcTestResult> results) {
        return results.stream()
                .collect(Collectors.groupingBy(Responses::outcome, TreeMap::new, Collectors.counting()));
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (BrokenBarrierException | TimeoutException e) {
            throw new IllegalStateException("The other thread didn't arrive", e);
        }
    }
}
