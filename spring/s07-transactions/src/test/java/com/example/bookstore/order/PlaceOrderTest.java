package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.List;
import java.util.Map;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link OrderService#placeOrder}: all or nothing.
 *
 * <p>These tests don't run in a test transaction ({@code NOT_SUPPORTED}): {@code placeOrder} must really commit, or
 * really roll back, before the test reads the tables with plain SQL. Inside a test transaction, a failed
 * {@code placeOrder} would only mark that transaction for rollback, and the test would still see its half-done
 * changes. Each test puts the seed data back afterwards.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S07-01 · Atomic operations with @Transactional")
class PlaceOrderTest {

    private static final long MINJI = 1;          // 100,000 won in her wallet
    private static final long ADMIN = 20;         // an empty wallet
    private static final long EFFECTIVE_JAVA = 1; // 36,000 won, 10 copies in stock
    private static final long CLEAN_CODE = 2;     // its price and stock are read from the database
    private static final long RANGE = 59;         // out of stock

    @Autowired
    private OrderService orderService;

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
        @DisplayName("takes the copies of every line out of stock")
        void decreasesTheStockOfEveryLine() {
            int cleanCodeStock = stockOf(CLEAN_CODE);

            orderService.placeOrder(MINJI, order("key-1", line(EFFECTIVE_JAVA, 2), line(CLEAN_CODE, 1)));

            assertThat(stockOf(EFFECTIVE_JAVA)).isEqualTo(8);
            assertThat(stockOf(CLEAN_CODE)).isEqualTo(cleanCodeStock - 1);
        }

        @Test
        @DisplayName("saves a PAID order with its lines, ordered now, and returns it")
        void savesAndReturnsThePaidOrder() {
            long cleanCodePrice = priceOf(CLEAN_CODE);

            OrderResponse order = orderService.placeOrder(MINJI,
                    order("key-1", line(EFFECTIVE_JAVA, 1), line(CLEAN_CODE, 1)));

            assertThat(order.status()).isEqualTo(OrderStatus.PAID);
            assertThat(order.totalAmount()).isEqualTo(36_000 + cleanCodePrice);
            assertThat(order.orderedAt()).isEqualTo(TestClock.DEFAULT_INSTANT);
            assertThat(order.lines()).containsExactly(
                    new OrderResponse.Line(EFFECTIVE_JAVA, 1, 36_000),
                    new OrderResponse.Line(CLEAN_CODE, 1, cleanCodePrice));
            assertThat(jdbc.sql("SELECT member_id, status, total_amount FROM orders WHERE id = ?")
                    .param(order.id()).query().singleRow())
                    .containsExactlyInAnyOrderEntriesOf(Map.of(
                            "member_id", MINJI, "status", "PAID", "total_amount", 36_000 + cleanCodePrice));
            assertThat(countWhere("order_lines", "order_id", order.id())).isEqualTo(2);
        }

        @Test
        @DisplayName("records an APPROVED payment of the total, with the request's idempotency key")
        void recordsTheApprovedPayment() {
            OrderResponse order = orderService.placeOrder(MINJI, order("key-1", line(EFFECTIVE_JAVA, 2)));

            assertThat(jdbc.sql("SELECT idempotency_key, amount, status FROM payments WHERE order_id = ?")
                    .param(order.id()).query().listOfRows())
                    .containsExactly(Map.of("idempotency_key", "key-1", "amount", 72_000L, "status", "APPROVED"));
        }

        @Test
        @DisplayName("takes the total from the wallet and records it as a PAYMENT wallet transaction")
        void chargesTheWallet() {
            orderService.placeOrder(MINJI, order("key-1", line(EFFECTIVE_JAVA, 2)));

            assertThat(balanceOf(MINJI)).isEqualTo(100_000 - 72_000);
            assertThat(jdbc.sql("""
                    SELECT t.type, t.amount FROM wallet_transactions t JOIN wallets w ON w.id = t.wallet_id
                    WHERE w.member_id = ? ORDER BY t.id DESC LIMIT 1""").param(MINJI).query().singleRow())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("type", "PAYMENT", "amount", 72_000L));
        }

        @Test
        @DisplayName("when the last step fails (an empty wallet), nothing changes: stock, orders, payments, wallet")
        void failureInTheLastStepChangesNothing() {
            Snapshot before = snapshot();

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> orderService.placeOrder(ADMIN,
                            order("key-1", line(EFFECTIVE_JAVA, 1), line(CLEAN_CODE, 1))))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
            assertThat(snapshot()).isEqualTo(before);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("when the wallet is a little short (3 copies cost 108,000 won), nothing changes either")
        void walletALittleShortChangesNothing() {
            Snapshot before = snapshot();

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> orderService.placeOrder(MINJI, order("key-1", line(EFFECTIVE_JAVA, 3))))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
            assertThat(snapshot()).isEqualTo(before);
        }

        @Test
        @DisplayName("when a later line is out of stock, the earlier lines keep their stock")
        void outOfStockOnALaterLineChangesNothing() {
            Snapshot before = snapshot();

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> orderService.placeOrder(MINJI,
                            order("key-1", line(EFFECTIVE_JAVA, 1), line(RANGE, 1))))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.OUT_OF_STOCK);
            assertThat(snapshot()).isEqualTo(before);
        }

        @Test
        @DisplayName("when a later line names a book that doesn't exist, nothing changes")
        void unknownBookOnALaterLineChangesNothing() {
            Snapshot before = snapshot();

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> orderService.placeOrder(MINJI,
                            order("key-1", line(EFFECTIVE_JAVA, 1), line(9_999, 1))))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.BOOK_NOT_FOUND);
            assertThat(snapshot()).isEqualTo(before);
        }

        @Test
        @DisplayName("two lines for the same book take both quantities out of stock and become one order line")
        void sameBookOnTwoLines() {
            OrderResponse order = orderService.placeOrder(MINJI,
                    order("key-1", line(EFFECTIVE_JAVA, 1), line(EFFECTIVE_JAVA, 1)));

            assertThat(stockOf(EFFECTIVE_JAVA)).isEqualTo(8);
            assertThat(order.lines()).containsExactly(new OrderResponse.Line(EFFECTIVE_JAVA, 2, 36_000));
            assertThat(balanceOf(MINJI)).isEqualTo(100_000 - 72_000);
        }

        @Test
        @DisplayName("an order may take the last copies and the last won")
        void lastCopiesAndLastWon() {
            jdbc.sql("UPDATE inventories SET quantity = 2 WHERE book_id = ?").param(EFFECTIVE_JAVA).update();
            jdbc.sql("UPDATE wallets SET balance = 72000 WHERE member_id = ?").param(MINJI).update();

            orderService.placeOrder(MINJI, order("key-1", line(EFFECTIVE_JAVA, 2)));

            assertThat(stockOf(EFFECTIVE_JAVA)).isZero();
            assertThat(balanceOf(MINJI)).isZero();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects an unknown member with MEMBER_NOT_FOUND, and changes nothing")
        void unknownMember() {
            Snapshot before = snapshot();

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> orderService.placeOrder(9_999, order("key-1", line(EFFECTIVE_JAVA, 1))))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
            assertThat(snapshot()).isEqualTo(before);
        }

        @Test
        @DisplayName("rejects an order without lines with IllegalArgumentException, and changes nothing")
        void noLines() {
            Snapshot before = snapshot();

            assertThatExceptionOfType(IllegalArgumentException.class)
                    .isThrownBy(() -> orderService.placeOrder(MINJI, new CreateOrderRequest("key-1", List.of())));
            assertThat(snapshot()).isEqualTo(before);
        }
    }

    /** Everything that {@code placeOrder} may change. Two equal snapshots mean that nothing changed. */
    private record Snapshot(int effectiveJavaStock, int cleanCodeStock, long orders, long orderLines, long payments,
                            long minjiBalance, long adminBalance, long walletTransactions) {
    }

    private Snapshot snapshot() {
        return new Snapshot(stockOf(EFFECTIVE_JAVA), stockOf(CLEAN_CODE), count("orders"), count("order_lines"),
                count("payments"), balanceOf(MINJI), balanceOf(ADMIN), count("wallet_transactions"));
    }

    private static CreateOrderRequest order(String idempotencyKey, CreateOrderRequest.Line... lines) {
        return new CreateOrderRequest(idempotencyKey, List.of(lines));
    }

    private static CreateOrderRequest.Line line(long bookId, int quantity) {
        return new CreateOrderRequest.Line(bookId, quantity);
    }

    private int stockOf(long bookId) {
        return jdbc.sql("SELECT quantity FROM inventories WHERE book_id = ?").param(bookId)
                .query(Integer.class).single();
    }

    private long priceOf(long bookId) {
        return jdbc.sql("SELECT price FROM books WHERE id = ?").param(bookId).query(Long.class).single();
    }

    private long balanceOf(long memberId) {
        return jdbc.sql("SELECT balance FROM wallets WHERE member_id = ?").param(memberId)
                .query(Long.class).single();
    }

    private long count(String table) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();
    }

    private long countWhere(String table, String column, long value) {
        return jdbc.sql("SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?").param(value)
                .query(Long.class).single();
    }
}
