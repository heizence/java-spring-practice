package com.example.bookstore.coupon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.coupon.CampaignReport.SkippedMember;

/** Gives a coupon to several members, one after the other. */
class CampaignIssuer {

    private final CouponService couponService;

    CampaignIssuer(CouponService couponService) {
        this.couponService = couponService;
    }

    CampaignReport issueToAll(String code, Collection<Long> memberIds) {
        List<Long> issued = new ArrayList<>();
        List<SkippedMember> skipped = new ArrayList<>();
        for (long memberId : memberIds) {
            try {
                couponService.issue(code, memberId);
                issued.add(memberId);
            } catch (BusinessException e) {
                skipped.add(new SkippedMember(memberId, e.getErrorCode()));
            }
        }
        return new CampaignReport(code, issued, skipped);
    }
}
