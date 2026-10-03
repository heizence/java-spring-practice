package com.example.bookstore.coupon;

import java.time.Instant;
import java.util.Optional;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import jakarta.persistence.Entity;
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
import org.springframework.util.Assert;

/**
 * A copy of a {@link Coupon} that a member received. Each member gets each coupon at most once, and uses it at most
 * once.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupon_issues")
public class CouponIssue {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id")
    private Coupon coupon;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    private Instant issuedAt;

    /** {@code null} until the coupon is used; read it through {@link #getUsedAt()}. */
    private Instant usedAt;

    /** Called by {@link Coupon#issue}, which checks the arguments. */
    CouponIssue(Coupon coupon, Member member, Instant issuedAt) {
        this.coupon = coupon;
        this.member = member;
        this.issuedAt = issuedAt;
    }

    /** When the coupon was used, or empty if it wasn't used yet. */
    public Optional<Instant> getUsedAt() {
        return Optional.ofNullable(usedAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    /**
     * Uses the coupon, for example to pay for an order.
     *
     * @param now the current time, from the {@code Clock} bean
     * @throws BusinessException {@link ErrorCode#COUPON_ALREADY_USED} if it was used before,
     *                           {@link ErrorCode#COUPON_NOT_YET_VALID} or {@link ErrorCode#COUPON_EXPIRED} if
     *                           {@code now} is outside the coupon's validity period
     */
    public void use(Instant now) {
        Assert.notNull(now, "now must not be null");
        if (usedAt != null) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_USED,
                    "Coupon %s was already used at %s.".formatted(coupon.getCode(), usedAt));
        }
        coupon.checkValidAt(now);
        this.usedAt = now;
    }
}
