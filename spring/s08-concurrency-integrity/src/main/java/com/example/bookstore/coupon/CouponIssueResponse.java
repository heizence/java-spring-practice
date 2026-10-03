package com.example.bookstore.coupon;

import java.time.Instant;

/**
 * A coupon that a member received, as the API shows it.
 *
 * @param discountAmount the discount in won
 */
public record CouponIssueResponse(Long id, String couponCode, Long memberId, long discountAmount, Instant issuedAt) {

    /** Reads the issue's coupon, so call it while the issue's persistence context is open. */
    static CouponIssueResponse from(CouponIssue issue) {
        return new CouponIssueResponse(issue.getId(), issue.getCoupon().getCode(), issue.getMember().getId(),
                issue.getCoupon().getDiscountAmount(), issue.getIssuedAt());
    }
}
