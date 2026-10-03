package com.example.bookstore.coupon;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.coupon.CampaignReport.SkippedMember;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Coupon campaigns: the marketing team gives one coupon to a list of members at once, for example "AUTUMN3000 for
 * everyone who ordered in September". Their tool calls {@link #issueToAll}.
 */
@Service
@RequiredArgsConstructor
public class CouponCampaignService {

    /** The most members one campaign run may name. Bigger campaigns run in several parts. */
    public static final int MAX_MEMBERS = 1_000;

    /** Spring injects the proxy of CouponService, not the object itself: every call on it goes through the advice. */
    private final CouponService couponService;

    /**
     * Gives a coupon to every member in the list.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>Each member gets the coupon exactly as if {@link CouponService#issue} had been called for them alone. So
     *       the audit log has one {@code CouponService.issue} entry per member: {@code SUCCEEDED}, or {@code FAILED}
     *       with the error code.</li>
     *   <li>A member who can't get the coupon (they already have it, it's sold out, …) is skipped with the reason,
     *       and the campaign goes on with the next member.</li>
     *   <li>Members are handled in the order of the list. A member listed twice is handled once.</li>
     * </ul>
     *
     * @return who got the coupon and who was skipped, in the order of the list
     * @throws BusinessException        {@link ErrorCode#COUPON_NOT_FOUND} if no coupon has this code; then nobody gets
     *                                  anything
     * @throws IllegalArgumentException if the list names more than {@value #MAX_MEMBERS} members
     */
    public CampaignReport issueToAll(String code, List<Long> memberIds) {
        Assert.notNull(memberIds, "memberIds must not be null");
        Assert.noNullElements(memberIds, "memberIds must not contain null");
        Assert.isTrue(memberIds.size() <= MAX_MEMBERS,
                () -> "A campaign can name at most %d members, not %d.".formatted(MAX_MEMBERS, memberIds.size()));
        couponService.findCoupon(code); // fails for an unknown code before anyone gets anything

        List<Long> issued = new ArrayList<>();
        List<SkippedMember> skipped = new ArrayList<>();
        for (long memberId : new LinkedHashSet<>(memberIds)) {
            try {
                // A call from this bean to another bean goes through that bean's proxy, so the audit advice runs
                // for each member. The loop used to live in CouponService, where this call was a call on "this".
                couponService.issue(code, memberId);
                issued.add(memberId);
            } catch (BusinessException e) {
                skipped.add(new SkippedMember(memberId, e.getErrorCode()));
            }
        }
        return new CampaignReport(code, issued, skipped);
    }
}
