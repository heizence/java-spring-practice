package com.example.bookstore.coupon;

import java.time.Instant;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.AccessLevel;
import lombok.Getter;
import org.springframework.util.Assert;

/**
 * A discount code with a limited number of copies and a validity period. Issuing gives one copy to a member as a
 * {@link CouponIssue}; at most {@code totalQuantity} copies are ever issued.
 *
 * <p>S03 has no database, so coupons are plain objects that {@link CouponRepository} keeps in memory. From S06 on,
 * {@code Coupon} is a JPA entity with the same rules.
 */
@Getter
public class Coupon {

    /** What members type in, such as {@code WELCOME5000}. Unique. */
    private final String code;

    /** The discount in won. */
    private final long discountAmount;

    private final int totalQuantity;

    /** The first moment the coupon is valid. */
    private final Instant validFrom;

    /** The last moment the coupon is valid (inclusive). */
    private final Instant validUntil;

    /** Changes when a copy is issued, so it's guarded by this object's lock (see {@link #issue}). */
    @Getter(AccessLevel.NONE)
    private int issuedQuantity;

    Coupon(String code, long discountAmount, int totalQuantity, int issuedQuantity, Instant validFrom,
           Instant validUntil) {
        Assert.hasText(code, "code must not be blank");
        Assert.isTrue(discountAmount > 0, () -> "discountAmount must be positive: " + discountAmount);
        Assert.isTrue(totalQuantity > 0, () -> "totalQuantity must be positive: " + totalQuantity);
        Assert.isTrue(issuedQuantity >= 0 && issuedQuantity <= totalQuantity,
                () -> "issuedQuantity must be between 0 and %d: %d".formatted(totalQuantity, issuedQuantity));
        Assert.notNull(validFrom, "validFrom must not be null");
        Assert.notNull(validUntil, "validUntil must not be null");
        this.code = code;
        this.discountAmount = discountAmount;
        this.totalQuantity = totalQuantity;
        this.issuedQuantity = issuedQuantity;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public synchronized int getIssuedQuantity() {
        return issuedQuantity;
    }

    /** How many copies can still be issued. */
    public synchronized int getRemainingQuantity() {
        return totalQuantity - issuedQuantity;
    }

    /**
     * Gives one copy of this coupon to a member. This method can't see which coupons the member already has; the
     * caller checks that first.
     *
     * @param now the current time, from the {@code Clock} bean
     * @return the new issue; save it
     * @throws BusinessException {@link ErrorCode#COUPON_NOT_YET_VALID} or {@link ErrorCode#COUPON_EXPIRED} if
     *                           {@code now} is outside the validity period, {@link ErrorCode#COUPON_SOLD_OUT} if no
     *                           copies are left; nothing changes in either case
     */
    synchronized CouponIssue issue(long memberId, Instant now) {
        Assert.notNull(now, "now must not be null");
        if (now.isBefore(validFrom)) {
            throw new BusinessException(ErrorCode.COUPON_NOT_YET_VALID,
                    "Coupon %s is valid from %s.".formatted(code, validFrom));
        }
        if (now.isAfter(validUntil)) {
            throw new BusinessException(ErrorCode.COUPON_EXPIRED,
                    "Coupon %s expired at %s.".formatted(code, validUntil));
        }
        if (issuedQuantity >= totalQuantity) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT,
                    "All %d copies of coupon %s have been issued.".formatted(totalQuantity, code));
        }
        issuedQuantity++;
        return new CouponIssue(code, memberId, now);
    }
}
