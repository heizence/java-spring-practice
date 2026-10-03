package com.example.bookstore.payment;

import java.time.Instant;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

/**
 * An attempt to pay for an {@link Order}.
 *
 * <p>The client sends an idempotency key with each payment request and sends the same key again when it retries.
 * The key is unique ({@code uk_payments_idempotency_key}), so a retried request can find the earlier payment instead
 * of charging the member twice.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "payments")
@EntityListeners(AuditingEntityListener.class)
public class Payment {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    public static final int MAX_IDEMPOTENCY_KEY_LENGTH = 64;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    private String idempotencyKey;

    /** In won. It can be less than the order's total, for example after a coupon discount. */
    private long amount;

    @Enumerated(EnumType.STRING)
    private PaymentStatus status;

    /** Filled by Spring Data auditing when the payment is saved. The table has no {@code updated_at}. */
    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    private Payment(Order order, String idempotencyKey, long amount, PaymentStatus status) {
        Assert.notNull(order, "order must not be null");
        Assert.hasText(idempotencyKey, "idempotencyKey must not be blank");
        Assert.isTrue(idempotencyKey.length() <= MAX_IDEMPOTENCY_KEY_LENGTH,
                () -> "idempotencyKey must be at most %d characters: %s"
                        .formatted(MAX_IDEMPOTENCY_KEY_LENGTH, idempotencyKey));
        Assert.isTrue(amount > 0, () -> "amount must be positive: " + amount);
        this.order = order;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.status = status;
    }

    /** Records a payment whose money was taken from the member's wallet. */
    public static Payment approved(Order order, String idempotencyKey, long amount) {
        return new Payment(order, idempotencyKey, amount, PaymentStatus.APPROVED);
    }

    /** Records a payment that didn't go through. */
    public static Payment failed(Order order, String idempotencyKey, long amount) {
        return new Payment(order, idempotencyKey, amount, PaymentStatus.FAILED);
    }

    /**
     * Marks an approved payment as refunded. Returning the money to the wallet is the caller's job.
     *
     * @throws BusinessException {@link ErrorCode#INVALID_PAYMENT_STATUS} unless the payment is {@code APPROVED}
     */
    public void refund() {
        if (status != PaymentStatus.APPROVED) {
            throw new BusinessException(ErrorCode.INVALID_PAYMENT_STATUS,
                    "Payment %s is %s; only an APPROVED payment can be refunded.".formatted(id, status));
        }
        this.status = PaymentStatus.REFUNDED;
    }
}
