package com.example.bookstore.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import com.example.bookstore.order.Order;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Template · Payment")
class PaymentTest {

    private final Order order = Order.place(Member.register("minji@example.com", "Minji Kim", "{noop}x"),
            Instant.parse("2026-10-15T00:00:00Z"));

    @Test
    @DisplayName("records an approved or a failed payment")
    void recordsTheOutcome() {
        Payment approved = Payment.approved(order, "key-1", 36_000);
        Payment failed = Payment.failed(order, "key-2", 36_000);

        assertThat(approved.getStatus()).isEqualTo(PaymentStatus.APPROVED);
        assertThat(approved.getOrder()).isSameAs(order);
        assertThat(approved.getIdempotencyKey()).isEqualTo("key-1");
        assertThat(approved.getAmount()).isEqualTo(36_000);
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("refund() turns an approved payment into a refunded one")
    void refundsAnApprovedPayment() {
        Payment payment = Payment.approved(order, "key-1", 36_000);

        payment.refund();

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    @DisplayName("refund() rejects a payment that isn't approved with INVALID_PAYMENT_STATUS")
    void rejectsRefundsOfOtherPayments() {
        Payment failed = Payment.failed(order, "key-1", 36_000);
        Payment refunded = Payment.approved(order, "key-2", 36_000);
        refunded.refund();

        assertThatThrownBy(failed::refund)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);
        assertThatThrownBy(refunded::refund)
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.INVALID_PAYMENT_STATUS);
        assertThat(failed.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    @DisplayName("accepts an idempotency key of up to 64 characters")
    void idempotencyKeyLength() {
        assertThat(Payment.approved(order, "k".repeat(64), 1).getIdempotencyKey()).hasSize(64);
        assertThatThrownBy(() -> Payment.approved(order, "k".repeat(65), 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Payment.approved(order, " ", 1)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest(name = "amount {0}")
    @ValueSource(longs = {0, -1})
    @DisplayName("rejects an amount below 1")
    void rejectsNonPositiveAmounts(long amount) {
        assertThatThrownBy(() -> Payment.approved(order, "key-1", amount))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
