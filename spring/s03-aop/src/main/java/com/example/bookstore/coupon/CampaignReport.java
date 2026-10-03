package com.example.bookstore.coupon;

import java.util.List;
import java.util.Objects;

import com.example.bookstore.common.error.ErrorCode;

/**
 * The result of a coupon campaign: who got the coupon and who didn't.
 *
 * @param couponCode the coupon that was issued
 * @param issued     the members who got it, in the order of the campaign's member list
 * @param skipped    the members who didn't, with the reason, in the same order
 */
public record CampaignReport(String couponCode, List<Long> issued, List<SkippedMember> skipped) {

    public CampaignReport {
        Objects.requireNonNull(couponCode, "couponCode");
        issued = List.copyOf(issued);
        skipped = List.copyOf(skipped);
    }

    /**
     * A member who didn't get the coupon.
     *
     * @param reason why not, such as {@link ErrorCode#COUPON_ALREADY_ISSUED}
     */
    public record SkippedMember(long memberId, ErrorCode reason) {

        public SkippedMember {
            Objects.requireNonNull(reason, "reason");
        }
    }
}
