package com.example.bookstore.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.TestClock;
import com.example.bookstore.wallet.Wallet;
import com.example.bookstore.wallet.WalletRepository;
import com.example.bookstore.wallet.WalletTransactionRepository;
import com.example.bookstore.wallet.WalletTransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link PaymentAttemptLog#recordFailure}: the log entry lives in a transaction of its own.
 *
 * <p>The tests don't run in a test transaction ({@code NOT_SUPPORTED}). Where a test needs a caller's transaction, it
 * starts one with a {@link TransactionTemplate}, which commits when the callback returns, and rolls back when it
 * throws or calls {@code status.setRollbackOnly()}. Each test puts the seed data back at the end.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S07-03 · Propagation")
class PaymentAttemptLogTest {

    private static final long MINJI = 1;          // 100,000 won in her wallet
    private static final long EFFECTIVE_JAVA = 1; // 36,000 won

    @Autowired
    private PaymentAttemptLog paymentAttemptLog;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private WalletTransactionRepository walletTransactionRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private Clock clock;

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
        @DisplayName("a declined payment leaves one entry in the log: order, member, amount, reason and time")
        void declinedPaymentIsLogged() {
            long orderId = pendingOrderOfMinji(3); // 108,000 won with 100,000 in the wallet

            assertThatThrownBy(() -> paymentService.pay(orderId, "pay-1"))
                    .isInstanceOf(PaymentDeclinedException.class);

            assertThat(attemptsOf(orderId)).containsExactly(Map.of(
                    "member_id", MINJI,
                    "amount", 108_000L,
                    "error_code", "INSUFFICIENT_BALANCE",
                    "attempted_at", TestClock.DEFAULT_INSTANT));
        }

        @Test
        @DisplayName("the entry survives when the caller's transaction rolls back")
        void entrySurvivesTheCallersRollback() {
            long orderId = pendingOrderOfMinji(1);

            transactionTemplate.executeWithoutResult(status -> {
                paymentAttemptLog.recordFailure(orderId, MINJI, 36_000, ErrorCode.INSUFFICIENT_BALANCE);
                status.setRollbackOnly(); // the caller's transaction ends in a rollback
            });

            assertThat(attemptsOf(orderId)).hasSize(1);
        }

        @Test
        @DisplayName("an approved payment leaves nothing in the log")
        void approvedPaymentIsNotLogged() throws Exception {
            long orderId = pendingOrderOfMinji(1);

            paymentService.pay(orderId, "pay-1");

            assertThat(attemptsOf(orderId)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the entry is committed at once: another session sees it while the caller's transaction runs")
        void entryIsVisibleToOtherSessionsAtOnce() {
            long orderId = pendingOrderOfMinji(1);

            Long seenByAnotherSession = transactionTemplate.execute(status -> {
                paymentAttemptLog.recordFailure(orderId, MINJI, 36_000, ErrorCode.INSUFFICIENT_BALANCE);
                return countAttemptsInAnotherSession(orderId); // the caller's transaction is still open here
            });

            assertThat(seenByAnotherSession).isEqualTo(1);
        }

        @Test
        @DisplayName("two failed attempts to pay the same order leave two entries, oldest first")
        void twoFailedAttempts() {
            long orderId = pendingOrderOfMinji(3);

            for (long amount : new long[] {108_000, 72_000}) {
                transactionTemplate.executeWithoutResult(status -> {
                    paymentAttemptLog.recordFailure(orderId, MINJI, amount, ErrorCode.INSUFFICIENT_BALANCE);
                    status.setRollbackOnly();
                });
            }

            assertThat(attemptsOf(orderId)).extracting(attempt -> attempt.get("amount"))
                    .containsExactly(108_000L, 72_000L);
        }
    }

    @Nested
    @DisplayName("The rollback-only trap")
    class RollbackOnlyTrap {

        /**
         * Not about your code: this test shows the trap. {@code saveAndFlush} runs in a transaction with the default
         * propagation, REQUIRED, so it joins the caller's transaction. When it fails, it marks that shared
         * transaction rollback-only before the exception reaches the caller. Catching the exception can't undo the
         * mark, and the caller's commit turns into a rollback.
         */
        @Test
        @DisplayName("REQUIRED: a caught failure still rolls back the caller's work (UnexpectedRollbackException)")
        void requiredDoomsTheCaller() {
            assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
                chargeMinjisWallet(5_000); // the caller's own work
                try {
                    // Minji's email is taken, so the insert fails inside save's joined transaction.
                    memberRepository.saveAndFlush(Member.register("minji@example.com", "Minji Again", "{noop}x"));
                } catch (DataIntegrityViolationException ignored) {
                    // "Not important, carry on." Too late: the shared transaction is already rollback-only.
                }
            })).isInstanceOf(UnexpectedRollbackException.class);

            assertThat(balanceOfMinji()).isEqualTo(100_000);
        }

        @Test
        @DisplayName("REQUIRES_NEW: when writing the log fails, the caller can catch it and still commit its work")
        void requiresNewKeepsTheCallerAlive() {
            long orderId = pendingOrderOfMinji(1);

            transactionTemplate.executeWithoutResult(status -> {
                chargeMinjisWallet(5_000); // the caller's own work
                assertThatThrownBy(() -> paymentAttemptLog.recordFailure(
                        orderId, MINJI, 0, ErrorCode.INSUFFICIENT_BALANCE)) // amount 0: the log refuses it
                        .isInstanceOf(IllegalArgumentException.class);
            });

            assertThat(balanceOfMinji()).isEqualTo(105_000);
            assertThat(attemptsOf(orderId)).isEmpty();
        }
    }

    /** Saves (and commits) a PENDING order of Minji's for some copies of Effective Java, at 36,000 won each. */
    private long pendingOrderOfMinji(int copies) {
        Long orderId = transactionTemplate.execute(status -> {
            Order order = Order.place(memberRepository.getReferenceById(MINJI), clock.instant());
            order.addLine(bookRepository.findById(EFFECTIVE_JAVA).orElseThrow(), copies);
            return orderRepository.save(order).getId();
        });
        assertThat(orderId).isNotNull();
        return orderId;
    }

    /** Joins the caller's transaction, if there is one. */
    private void chargeMinjisWallet(long amount) {
        Wallet wallet = walletRepository.findByMemberId(MINJI).orElseThrow();
        walletTransactionRepository.save(wallet.deposit(amount, WalletTransactionType.CHARGE));
    }

    /** The log entries of an order, oldest first, without their ids and order ids. */
    private List<Map<String, Object>> attemptsOf(long orderId) {
        return jdbc.sql("""
                        SELECT member_id, amount, error_code, attempted_at FROM payment_attempts
                        WHERE order_id = ? ORDER BY id""")
                .param(orderId)
                .query((row, rowNumber) -> Map.<String, Object>of(
                        "member_id", row.getLong("member_id"),
                        "amount", row.getLong("amount"),
                        "error_code", row.getString("error_code"),
                        "attempted_at", row.getTimestamp("attempted_at").toInstant()))
                .list();
    }

    /**
     * Counts with a connection of its own, straight from the pool. Spring doesn't hand it the caller's transaction,
     * so it sees only what other transactions have committed.
     */
    private long countAttemptsInAnotherSession(long orderId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT COUNT(*) FROM payment_attempts WHERE order_id = ?")) {
            statement.setLong(1, orderId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getLong(1);
            }
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private long balanceOfMinji() {
        return jdbc.sql("SELECT balance FROM wallets WHERE member_id = ?").param(MINJI).query(Long.class).single();
    }
}
