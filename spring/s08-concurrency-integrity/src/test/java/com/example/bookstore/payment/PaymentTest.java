package com.example.bookstore.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
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
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Paying an order: {@code POST /api/orders/{orderId}/payments} with an {@code Idempotency-Key} header. Each test
 * places its own orders of book 1 (36,000 won). Minji (member 1) has 100,000 won and the admin (member 20) has 0
 * ({@code spring/BOOKSTORE.md}).
 *
 * <p>Every request commits, as in production, and each test puts the seed data back afterwards.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S08-06 · Idempotent payments")
class PaymentTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

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
        @DisplayName("paying a pending order takes the money from the wallet, marks the order PAID and answers 201")
        void paysAnOrder() {
            long orderId = pendingOrderOf(1);
            long paymentsBefore = walletPaymentsOf(1);

            MvcTestResult result = pay(orderId, "key-1");

            assertThat(result)
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson().isLenientlyEqualTo("""
                            {
                              "orderId": %d,
                              "amount": 36000,
                              "status": "APPROVED",
                              "createdAt": "%s"
                            }""".formatted(orderId, TestClock.DEFAULT_INSTANT));
            assertThat(result).headers().hasValue(HttpHeaders.LOCATION, "/api/payments/" + paymentIdIn(result));
            assertThat(balanceOf(1)).isEqualTo(64_000);
            assertThat(statusOf(orderId)).isEqualTo("PAID");
            assertThat(walletPaymentsOf(1)).isEqualTo(paymentsBefore + 1);
        }

        @Test
        @DisplayName("repeating the request with the same key answers 200 with the same payment, and charges nothing")
        void repeatedRequest() {
            long orderId = pendingOrderOf(1);
            long paymentsBefore = walletPaymentsOf(1);
            MvcTestResult first = pay(orderId, "key-1");

            MvcTestResult second = pay(orderId, "key-1");

            assertThat(second).hasStatusOk();
            assertThat(Responses.body(second)).isEqualTo(Responses.body(first));
            assertThat(balanceOf(1)).isEqualTo(64_000);
            assertThat(paymentsWithKey("key-1")).isEqualTo(1);
            assertThat(walletPaymentsOf(1)).isEqualTo(paymentsBefore + 1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a third request with the same key, after a while, still gets the same payment")
        void repeatedLater() {
            long orderId = pendingOrderOf(1);
            long paymentId = paymentIdIn(pay(orderId, "key-1"));
            pay(orderId, "key-1");
            clock.advance(Duration.ofHours(1));

            MvcTestResult third = pay(orderId, "key-1");

            assertThat(third).hasStatusOk();
            assertThat(paymentIdIn(third)).isEqualTo(paymentId);
            assertThat(balanceOf(1)).isEqualTo(64_000);
        }

        @Test
        @DisplayName("each order is paid with its own key: two orders, two keys, two payments")
        void twoOrdersTwoKeys() {
            long first = pendingOrderOf(1);
            long second = pendingOrderOf(1);

            assertThat(pay(first, "key-1")).hasStatus(HttpStatus.CREATED);
            assertThat(pay(second, "key-2")).hasStatus(HttpStatus.CREATED);

            assertThat(balanceOf(1)).isEqualTo(28_000);
        }

        @Test
        @DisplayName("a new key for an order that is already paid fails with 409 INVALID_ORDER_STATUS, "
                + "and charges nothing")
        void newKeyForAPaidOrder() {
            long orderId = pendingOrderOf(1);
            pay(orderId, "key-1");

            MvcTestResult result = pay(orderId, "key-2");

            assertThat(Responses.outcome(result)).isEqualTo("409 INVALID_ORDER_STATUS");
            assertThat(balanceOf(1)).isEqualTo(64_000);
            assertThat(paymentsWithKey("key-2")).isZero();
        }

        @Test
        @DisplayName("a key that was used for another order fails with 409 IDEMPOTENCY_KEY_REUSED")
        void keyReusedForAnotherOrder() {
            long first = pendingOrderOf(1);
            long second = pendingOrderOf(1);
            pay(first, "key-1");

            MvcTestResult result = pay(second, "key-1");

            assertThat(Responses.outcome(result)).isEqualTo("409 IDEMPOTENCY_KEY_REUSED");
            assertThat(statusOf(second)).isEqualTo("PENDING");
            assertThat(balanceOf(1)).isEqualTo(64_000);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a member who can't afford the order gets 409 INSUFFICIENT_BALANCE, and nothing is recorded")
        void insufficientBalance() {
            long orderId = pendingOrderOf(20);

            MvcTestResult result = pay(orderId, "key-1");

            assertThat(Responses.outcome(result)).isEqualTo("409 INSUFFICIENT_BALANCE");
            assertThat(paymentsWithKey("key-1")).isZero();
            assertThat(statusOf(orderId)).isEqualTo("PENDING");
            assertThat(balanceOf(20)).isZero();
        }

        @Test
        @DisplayName("after a failed payment, the same key can be used again")
        void keyCanBeUsedAgainAfterAFailure() {
            long orderId = pendingOrderOf(20);
            pay(orderId, "key-1");
            jdbc.sql("UPDATE wallets SET balance = 50000 WHERE member_id = 20").update(); // the admin charges 50,000

            MvcTestResult retry = pay(orderId, "key-1");

            assertThat(retry).hasStatus(HttpStatus.CREATED);
            assertThat(balanceOf(20)).isEqualTo(14_000);
        }

        @Test
        @DisplayName("paying an order that doesn't exist fails with 404 ORDER_NOT_FOUND")
        void unknownOrder() {
            assertThat(Responses.outcome(pay(9_999, "key-1"))).isEqualTo("404 ORDER_NOT_FOUND");
            assertThat(paymentsWithKey("key-1")).isZero();
        }

        @Test
        @DisplayName("a request without an Idempotency-Key header is invalid input")
        void missingKey() {
            long orderId = pendingOrderOf(1);

            MvcTestResult result = mvc.post().uri("/api/orders/{orderId}/payments", orderId).exchange();

            assertThat(Responses.outcome(result)).isEqualTo("400 INVALID_INPUT");
            assertThat(balanceOf(1)).isEqualTo(100_000);
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("10 requests with the same key at the same moment all get the same payment, "
                + "and the wallet is charged once")
        void sameKeyAtTheSameMoment() {
            long orderId = pendingOrderOf(1);
            long paymentsBefore = walletPaymentsOf(1);

            Responses responses = Responses.sendAtOnce(10, i -> pay(orderId, "key-1"));

            assertThat(responses.countByOutcome()).isEqualTo(Map.of("201", 1L, "200", 9L));
            Set<Long> paymentIds = responses.all().stream()
                    .map(PaymentTest::paymentIdIn)
                    .collect(Collectors.toSet());
            assertThat(paymentIds).as("payment ids in the responses").hasSize(1);
            assertThat(paymentsWithKey("key-1")).isEqualTo(1);
            assertThat(balanceOf(1)).isEqualTo(64_000);
            assertThat(walletPaymentsOf(1)).isEqualTo(paymentsBefore + 1);
            assertThat(statusOf(orderId)).isEqualTo("PAID");
        }

        @Test
        @DisplayName("5 requests with different keys for the same order at the same moment: one pays, "
                + "the others get 409 INVALID_ORDER_STATUS")
        void differentKeysAtTheSameMoment() {
            long orderId = pendingOrderOf(1);

            Responses responses = Responses.sendAtOnce(5, i -> pay(orderId, "key-" + i));

            assertThat(responses.countByOutcome()).isEqualTo(Map.of("201", 1L, "409 INVALID_ORDER_STATUS", 4L));
            assertThat(balanceOf(1)).isEqualTo(64_000);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM payments WHERE order_id = ?").param(orderId)
                    .query(Long.class).single()).as("payments of the order").isEqualTo(1);
        }
    }

    private MvcTestResult pay(long orderId, String idempotencyKey) {
        return mvc.post().uri("/api/orders/{orderId}/payments", orderId)
                .header("Idempotency-Key", idempotencyKey)
                .exchange();
    }

    /** Places a {@code PENDING} order of one copy of book 1 (36,000 won), without changing the stock. */
    private long pendingOrderOf(long memberId) {
        return transactionTemplate.execute(status -> {
            Order order = Order.place(memberRepository.getReferenceById(memberId), clock.instant());
            order.addLine(bookRepository.findById(1L).orElseThrow(), 1);
            return orderRepository.save(order).getId();
        });
    }

    private static long paymentIdIn(MvcTestResult result) {
        Number id = JsonPath.read(Responses.body(result), "$.id");
        return id.longValue();
    }

    private long balanceOf(long memberId) {
        return jdbc.sql("SELECT balance FROM wallets WHERE member_id = ?").param(memberId).query(Long.class).single();
    }

    private String statusOf(long orderId) {
        return jdbc.sql("SELECT status FROM orders WHERE id = ?").param(orderId).query(String.class).single();
    }

    private long paymentsWithKey(String idempotencyKey) {
        return jdbc.sql("SELECT COUNT(*) FROM payments WHERE idempotency_key = ?").param(idempotencyKey)
                .query(Long.class).single();
    }

    /** How many {@code PAYMENT} transactions the member's wallet has. */
    private long walletPaymentsOf(long memberId) {
        return jdbc.sql("""
                SELECT COUNT(*) FROM wallet_transactions t JOIN wallets w ON w.id = t.wallet_id
                WHERE w.member_id = ? AND t.type = 'PAYMENT'""")
                .param(memberId).query(Long.class).single();
    }
}
