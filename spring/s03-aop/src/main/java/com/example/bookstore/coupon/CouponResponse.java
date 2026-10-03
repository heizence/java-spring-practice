package com.example.bookstore.coupon;

import java.time.Instant;

/**
 * A coupon as the API shows it.
 *
 * @param discountAmount    the discount in won
 * @param remainingQuantity how many copies can still be issued
 */
public record CouponResponse(String code, long discountAmount, int remainingQuantity, Instant validFrom,
                             Instant validUntil) {

    static CouponResponse from(Coupon coupon) {
        return new CouponResponse(coupon.getCode(), coupon.getDiscountAmount(), coupon.getRemainingQuantity(),
                coupon.getValidFrom(), coupon.getValidUntil());
    }
}
