package com.example.bookstore.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.wallet.Wallet;
import com.example.bookstore.wallet.WalletRepository;
import com.example.bookstore.wallet.WalletTransactionRepository;
import com.example.bookstore.wallet.WalletTransactionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link PaymentService#pay}: a declined payment must leave nothing behind.
 *
 * <p>The tests don't run in a test transaction ({@code NOT_SUPPORTED}): {@code pay} must really commit or roll back,
 * and the tests check what the database holds afterwards. Each test creates the orders it pays, and puts the seed
 * data back at the end.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S07-02 · Rollback rules")
class PaymentRollbackTest {

    private static final long MINJI = 1;          // 100,000 won in her wallet
    private static final long EFFECTIVE_JAVA = 1; // 36,000 won

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
        @DisplayName("pays a pending order: an APPROVED payment, a PAID order and the money taken from the wallet")
        void paysAPendingOrder() throws Exception {
            long orderId = pendingOrderOfMinji(1);

            PaymentResponse payment = paymentService.pay(orderId, "pay-1");

            assertThat(payment.orderId()).isEqualTo(orderId);
            assertThat(payment.amount()).isEqualTo(36_000);
            assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(statusOf(orderId)).isEqualTo("PAID");
            assertThat(approvedPaymentsOf(orderId)).isEqualTo(1);
            assertThat(balanceOfMinji()).isEqualTo(100_000 - 36_000);
        }

        @Test
        @DisplayName("a declined payment (108,000 won with 100,000 in the wallet) throws PaymentDeclinedException")
        void declinedPaymentThrows() {
            long orderId = pendingOrderOfMinji(3);

            assertThatThrownBy(() -> paymentService.pay(orderId, "pay-1"))
                    .isInstanceOfSatisfying(PaymentDeclinedException.class,
                            declined -> assertThat(declined.getOrderId()).isEqualTo(orderId));
        }

        @Test
        @DisplayName("a declined payment leaves the order PENDING")
        void declinedPaymentLeavesTheOrderPending() {
            long orderId = pendingOrderOfMinji(3);

            assertThatThrownBy(() -> paymentService.pay(orderId, "pay-1")).isInstanceOf(PaymentDeclinedException.class);

            assertThat(statusOf(orderId)).isEqualTo("PENDING");
        }

        @Test
        @DisplayName("a declined payment leaves no APPROVED payment behind")
        void declinedPaymentLeavesNoPayment() {
            long orderId = pendingOrderOfMinji(3);

            assertThatThrownBy(() -> paymentService.pay(orderId, "pay-1")).isInstanceOf(PaymentDeclinedException.class);

            assertThat(approvedPaymentsOf(orderId)).isZero();
        }

        @Test
        @DisplayName("a declined payment leaves the wallet as it was")
        void declinedPaymentLeavesTheWalletAlone() {
            long orderId = pendingOrderOfMinji(3);
            long walletTransactions = countWalletTransactions();

            assertThatThrownBy(() -> paymentService.pay(orderId, "pay-1")).isInstanceOf(PaymentDeclinedException.class);

            assertThat(balanceOfMinji()).isEqualTo(100_000);
            assertThat(countWalletTransactions()).isEqualTo(walletTransactions);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("after a declined payment, the order can be paid once the wallet has enough money")
        void canPayAfterADeclinedPayment() throws Exception {
            long orderId = pendingOrderOfMinji(3);
            assertThatThrownBy(() -> paymentService.pay(orderId, "pay-1")).isInstanceOf(PaymentDeclinedException.class);
            chargeMinjisWallet(50_000);

            PaymentResponse payment = paymentService.pay(orderId, "pay-2");

            assertThat(payment.status()).isEqualTo(PaymentStatus.APPROVED);
            assertThat(statusOf(orderId)).isEqualTo("PAID");
            assertThat(approvedPaymentsOf(orderId)).isEqualTo(1);
            assertThat(balanceOfMinji()).isEqualTo(150_000 - 108_000);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects an unknown order with ORDER_NOT_FOUND")
        void unknownOrder() {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> paymentService.pay(9_999, "pay-1"))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("rejects an order that is already PAID (an unchecked exception) and saves no second payment")
        void alreadyPaidOrder() throws Exception {
            long orderId = pendingOrderOfMinji(1);
            paymentService.pay(orderId, "pay-1");

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> paymentService.pay(orderId, "pay-2"))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
            assertThat(approvedPaymentsOf(orderId)).isEqualTo(1);
            assertThat(balanceOfMinji()).isEqualTo(100_000 - 36_000);
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

    private void chargeMinjisWallet(long amount) {
        transactionTemplate.executeWithoutResult(status -> {
            Wallet wallet = walletRepository.findByMemberId(MINJI).orElseThrow();
            walletTransactionRepository.save(wallet.deposit(amount, WalletTransactionType.CHARGE));
        });
    }

    private String statusOf(long orderId) {
        return jdbc.sql("SELECT status FROM orders WHERE id = ?").param(orderId).query(String.class).single();
    }

    private long approvedPaymentsOf(long orderId) {
        return jdbc.sql("SELECT COUNT(*) FROM payments WHERE order_id = ? AND status = 'APPROVED'").param(orderId)
                .query(Long.class).single();
    }

    private long balanceOfMinji() {
        return jdbc.sql("SELECT balance FROM wallets WHERE member_id = ?").param(MINJI).query(Long.class).single();
    }

    private long countWalletTransactions() {
        return jdbc.sql("SELECT COUNT(*) FROM wallet_transactions").query(Long.class).single();
    }
}
