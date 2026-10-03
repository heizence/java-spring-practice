package com.example.bookstore.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.time.Clock;
import java.util.List;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.payment.OrderPaid;
import com.example.bookstore.payment.PaymentResponse;
import com.example.bookstore.payment.PaymentService;
import com.example.bookstore.payment.PaymentStatus;
import com.example.bookstore.support.FakeMailer;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link PaymentService#pay} publishes {@link OrderPaid}, and {@link OrderConfirmationListener} emails the member, but
 * only after the commit.
 *
 * <p>No test transaction ({@code NOT_SUPPORTED}): after-commit listeners need a real commit. Where a test needs a
 * caller's transaction around the payment, it starts one with a {@link TransactionTemplate}. {@link FakeMailer}
 * records the emails instead of sending them. {@code @RecordApplicationEvents} records the published events. Each
 * test puts the seed data back at the end.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@RecordApplicationEvents
@DisplayName("S09-01 · After-commit events")
class OrderConfirmationTest {

    private static final long MINJI = 1;          // minji@example.com, 100,000 won in her wallet
    private static final long EFFECTIVE_JAVA = 1; // 36,000 won

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private FakeMailer mailer;

    @Autowired
    private ApplicationEvents events;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

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
        @DisplayName("publishes OrderPaid with the order, the member, the email address, the amount and the time")
        void publishesOrderPaid() {
            long orderId = pendingOrderOfMinji(1);

            paymentService.pay(orderId, "pay-1");

            assertThat(events.stream(OrderPaid.class)).containsExactly(
                    new OrderPaid(orderId, MINJI, "minji@example.com", 36_000, TestClock.DEFAULT_INSTANT));
        }

        @Test
        @DisplayName("sends one confirmation to the member after the payment commits")
        void sendsAConfirmation() {
            long orderId = pendingOrderOfMinji(1);

            paymentService.pay(orderId, "pay-1");

            assertThat(mailer.sent()).containsExactly(new Email("minji@example.com",
                    "Order %d is paid".formatted(orderId),
                    "We received your payment of 36000 won for order %d. Thank you!".formatted(orderId)));
        }

        @Test
        @DisplayName("sends nothing when the transaction around the payment rolls back")
        void sendsNothingAfterARollback() {
            long orderId = pendingOrderOfMinji(1);

            transactionTemplate.executeWithoutResult(status -> {
                paymentService.pay(orderId, "pay-1");
                status.setRollbackOnly(); // the caller's transaction, which pay() joined, ends in a rollback
            });

            assertThat(mailer.sent()).isEmpty();
            assertThat(statusOf(orderId)).isEqualTo("PENDING");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("sends nothing while the transaction around the payment still runs, and sends after its commit")
        void waitsForTheCommit() {
            long orderId = pendingOrderOfMinji(1);

            List<Email> sentBeforeTheCommit = transactionTemplate.execute(status -> {
                paymentService.pay(orderId, "pay-1");
                return mailer.sent();
            });

            assertThat(sentBeforeTheCommit).isEmpty();
            assertThat(mailer.sent()).hasSize(1);
        }

        @Test
        @DisplayName("sends nothing for a declined payment (108,000 won with 100,000 in the wallet)")
        void sendsNothingForADeclinedPayment() {
            long orderId = pendingOrderOfMinji(3);

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> paymentService.pay(orderId, "pay-1"))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
            assertThat(mailer.sent()).isEmpty();
        }

        @Test
        @DisplayName("a failing mailer doesn't undo the payment: pay() returns, and the order stays PAID")
        void mailerFailureKeepsThePayment() {
            long orderId = pendingOrderOfMinji(1);
            mailer.failWith(new IllegalStateException("The email provider is down"));

            PaymentResponse payment = paymentService.pay(orderId, "pay-1");

            assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(statusOf(orderId)).isEqualTo("PAID");
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

    private String statusOf(long orderId) {
        return jdbc.sql("SELECT status FROM orders WHERE id = ?").param(orderId).query(String.class).single();
    }
}
