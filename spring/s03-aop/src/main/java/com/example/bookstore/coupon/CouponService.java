package com.example.bookstore.coupon;

import java.time.Clock;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

/**
 * Use cases for coupons: looking one up, and giving copies to members.
 *
 * <p>Like the first version of every coupon service, {@link #issue} isn't safe when two requests give out the same
 * coupon at the same moment: both could pass the checks before either saves. S08 fixes that with the database.
 */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final Clock clock;

    /**
     * @throws BusinessException        {@link ErrorCode#COUPON_NOT_FOUND} if no coupon has this code
     * @throws IllegalArgumentException if the code is blank
     */
    public CouponResponse findCoupon(String code) {
        return CouponResponse.from(coupon(code));
    }

    /**
     * Gives one copy of a coupon to a member.
     *
     * @return the new issue
     * @throws BusinessException        {@link ErrorCode#COUPON_NOT_FOUND} if no coupon has this code,
     *                                  {@link ErrorCode#COUPON_ALREADY_ISSUED} if the member already has it,
     *                                  {@link ErrorCode#COUPON_NOT_YET_VALID} or {@link ErrorCode#COUPON_EXPIRED}
     *                                  outside its validity period, {@link ErrorCode#COUPON_SOLD_OUT} if no copies
     *                                  are left. Nothing changes in these cases.
     * @throws IllegalArgumentException if the code is blank or the member id isn't positive
     */
    public CouponIssue issue(String code, long memberId) {
        Assert.isTrue(memberId > 0, () -> "memberId must be positive: " + memberId);
        Coupon coupon = coupon(code);
        if (couponRepository.existsIssue(coupon.getCode(), memberId)) {
            throw new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED,
                    "Member %d already has coupon %s.".formatted(memberId, code));
        }
        CouponIssue issue = coupon.issue(memberId, clock.instant());
        couponRepository.saveIssue(issue);
        return issue;
    }

    // issueToAll() moved to CouponCampaignService. Here, its call issue(code, memberId) meant this.issue(...): a plain
    // Java call inside the object, which never passes through the proxy, so the audit advice never saw it.

    private Coupon coupon(String code) {
        Assert.hasText(code, "code must not be blank");
        return couponRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND,
                        "Coupon %s was not found.".formatted(code)));
    }
}
