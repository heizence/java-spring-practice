package com.example.bookstore.coupon;

import jakarta.validation.constraints.NotNull;

/**
 * The body of {@code POST /api/coupons/{code}/issues}: {@code { "memberId": 1 }}.
 *
 * @param memberId who gets the coupon. Until sign-in arrives (S12), the client says who it is.
 */
public record IssueCouponRequest(@NotNull Long memberId) {
}
