package com.example.bookstore.payment;

import java.time.Instant;

import com.example.bookstore.common.error.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

/**
 * One failed attempt to pay for an order, kept for compliance. Never changed after it is saved.
 *
 * <p>The order and the member are plain ids, not {@code @ManyToOne} associations: the log must not depend on those
 * rows (the table has no foreign keys; {@code V3__create_payment_attempts.sql} explains why).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Immutable
@Table(name = "payment_attempts")
@EntityListeners(AuditingEntityListener.class)
public class PaymentAttempt {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long orderId;

    private Long memberId;

    /** The amount that couldn't be paid, in won. */
    private long amount;

    /** Why the payment failed, such as {@link ErrorCode#INSUFFICIENT_BALANCE}. */
    @Enumerated(EnumType.STRING)
    private ErrorCode errorCode;

    /** Filled by Spring Data auditing from the {@code Clock} when the attempt is saved. */
    @CreatedDate
    @Column(updatable = false)
    private Instant attemptedAt;

    private PaymentAttempt(long orderId, long memberId, long amount, ErrorCode errorCode) {
        Assert.isTrue(amount > 0, () -> "amount must be positive: " + amount);
        Assert.notNull(errorCode, "errorCode must not be null");
        this.orderId = orderId;
        this.memberId = memberId;
        this.amount = amount;
        this.errorCode = errorCode;
    }

    /** Records a payment of {@code amount} won for an order that failed with {@code errorCode}. */
    public static PaymentAttempt failed(long orderId, long memberId, long amount, ErrorCode errorCode) {
        return new PaymentAttempt(orderId, memberId, amount, errorCode);
    }
}
