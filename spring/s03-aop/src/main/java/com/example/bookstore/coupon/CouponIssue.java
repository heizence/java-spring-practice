package com.example.bookstore.coupon;

import java.time.Instant;
import java.util.Objects;

/**
 * A copy of a {@link Coupon} that a member received. Each member gets each coupon at most once.
 *
 * @param couponCode the coupon's code, such as {@code AUTUMN3000}
 * @param memberId   the member who received it
 * @param issuedAt   when they received it
 */
public record CouponIssue(String couponCode, long memberId, Instant issuedAt) {

    public CouponIssue {
        Objects.requireNonNull(couponCode, "couponCode");
        Objects.requireNonNull(issuedAt, "issuedAt");
    }
}
