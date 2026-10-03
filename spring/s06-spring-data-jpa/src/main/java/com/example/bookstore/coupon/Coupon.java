package com.example.bookstore.coupon;

import java.time.Instant;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * A discount code with a limited number of copies and a validity period. Issuing gives one copy to a member as a
 * {@link CouponIssue}; at most {@code totalQuantity} copies are ever issued.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "coupons")
public class Coupon {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** What members type in, such as {@code WELCOME5000}. Unique. */
    private String code;

    /** The discount in won. */
    private long discountAmount;

    private int totalQuantity;

    private int issuedQuantity;

    /** The first moment the coupon is valid. */
    private Instant validFrom;

    /** The last moment the coupon is valid (inclusive). */
    private Instant validUntil;

    /** For optimistic locking: Hibernate increases it on every update and rejects updates based on an old value. */
    @Version
    private Long version;

    private Coupon(String code, long discountAmount, int totalQuantity, Instant validFrom, Instant validUntil) {
        Assert.hasText(code, "code must not be blank");
        Assert.isTrue(discountAmount > 0, () -> "discountAmount must be positive: " + discountAmount);
        Assert.isTrue(totalQuantity > 0, () -> "totalQuantity must be positive: " + totalQuantity);
        Assert.notNull(validFrom, "validFrom must not be null");
        Assert.notNull(validUntil, "validUntil must not be null");
        Assert.isTrue(!validUntil.isBefore(validFrom),
                () -> "validUntil (%s) must not be before validFrom (%s)".formatted(validUntil, validFrom));
        this.code = code.strip();
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public static Coupon create(String code, long discountAmount, int totalQuantity, Instant validFrom,
                                Instant validUntil) {
        return new Coupon(code, discountAmount, totalQuantity, validFrom, validUntil);
    }

    /**
     * Gives one copy of this coupon to a member.
     *
     * <p>This method can't see the coupons a member already has, so it doesn't check for duplicates: the caller
     * checks with {@code CouponIssueRepository} and reports {@link ErrorCode#COUPON_ALREADY_ISSUED}, and the unique
     * key {@code uk_coupon_issues_coupon_member} stops any duplicate that slips through.
     *
     * @param now the current time, from the {@code Clock} bean
     * @return the new issue; save it
     * @throws BusinessException {@link ErrorCode#COUPON_NOT_YET_VALID} or {@link ErrorCode#COUPON_EXPIRED} if
     *                           {@code now} is outside the validity period, {@link ErrorCode#COUPON_SOLD_OUT} if no
     *                           copies are left; nothing changes in either case
     */
    public CouponIssue issue(Member member, Instant now) {
        Assert.notNull(member, "member must not be null");
        checkValidAt(now);
        if (issuedQuantity >= totalQuantity) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT,
                    "All %d copies of coupon %s have been issued.".formatted(totalQuantity, code));
        }
        issuedQuantity++;
        return new CouponIssue(this, member, now);
    }

    /** How many copies can still be issued. */
    public int getRemainingQuantity() {
        return totalQuantity - issuedQuantity;
    }

    /** Whether the coupon can be issued or used at this moment (both ends of the period included). */
    public boolean isValidAt(Instant now) {
        Assert.notNull(now, "now must not be null");
        return !now.isBefore(validFrom) && !now.isAfter(validUntil);
    }

    /**
     * @throws BusinessException {@link ErrorCode#COUPON_NOT_YET_VALID} before the period,
     *                           {@link ErrorCode#COUPON_EXPIRED} after it
     */
    void checkValidAt(Instant now) {
        Assert.notNull(now, "now must not be null");
        if (now.isBefore(validFrom)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_YET_VALID,
                    "Coupon %s is valid from %s.".formatted(code, validFrom));
        }
        if (now.isAfter(validUntil)) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED,
                    "Coupon %s expired at %s.".formatted(code, validUntil));
        }
    }
}
