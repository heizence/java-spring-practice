package com.example.bookstore.coupon;

import java.time.Clock;
import java.time.Instant;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for coupons. */
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponIssueRepository couponIssueRepository;
    private final MemberRepository memberRepository;
    private final Clock clock;

    /**
     * Gives a member one copy of a coupon, first come, first served.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>The coupon must be valid now (the {@code Clock} bean's time): before its period, the request fails with
     *       {@link ErrorCode#COUPON_NOT_YET_VALID}; after it, with {@link ErrorCode#COUPON_EXPIRED}.</li>
     *   <li>A coupon has a limited number of copies ({@code totalQuantity}). However many requests arrive at the same
     *       moment, exactly that many copies are issued, never more. The requests that come too late fail with
     *       {@link ErrorCode#COUPON_SOLD_OUT}.</li>
     *   <li>A member gets each coupon at most once, even when the same member sends several requests at the same
     *       moment. The others fail with {@link ErrorCode#COUPON_ALREADY_ISSUED}, never with a {@code 500}. The
     *       unique key {@code uk_coupon_issues_coupon_member} is the final guard against a second copy.</li>
     *   <li>The coupon's {@code issuedQuantity} always equals the number of copies issued.</li>
     *   <li>A request that fails changes nothing.</li>
     * </ul>
     *
     * @return the new coupon issue
     * @throws BusinessException {@link ErrorCode#COUPON_NOT_FOUND} or {@link ErrorCode#MEMBER_NOT_FOUND} if the
     *                           coupon or the member doesn't exist, and the codes in the rules above
     */
    @Transactional
    public CouponIssueResponse issue(String code, long memberId) {
        Coupon coupon = couponRepository.findByCode(code)
                .orElseThrow(() -> new BusinessException(ErrorCode.COUPON_NOT_FOUND,
                        "Coupon %s was not found.".formatted(code)));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(memberId)));
        Instant now = clock.instant();
        coupon.checkValidAt(now);

        // A quick check for the usual case. It can't stop two requests that run at the same moment: both see "no
        // copy yet". The unique key below stops the second one.
        if (couponIssueRepository.existsByCouponIdAndMemberId(coupon.getId(), memberId)) {
            throw alreadyIssued(code, memberId);
        }
        if (coupon.getRemainingQuantity() <= 0) {
            throw new BusinessException(ErrorCode.COUPON_SOLD_OUT,
                    "All %d copies of coupon %s have been issued.".formatted(coupon.getTotalQuantity(), code));
        }
        // The database adds 1 to the current count, so no count gets lost.
        couponRepository.countIssuedCopy(coupon.getId());
        try {
            return CouponIssueResponse.from(couponIssueRepository.saveAndFlush(new CouponIssue(coupon, member, now)));
        } catch (DataIntegrityViolationException e) {
            // A request of the same member got its copy first. Throwing rolls back this transaction, including the
            // copy that takeCopy counted.
            throw alreadyIssued(code, memberId);
        }
    }

    /** @throws BusinessException {@link ErrorCode#NOT_FOUND} if no coupon issue has this id */
    @Transactional(readOnly = true)
    public CouponIssueResponse findIssue(long id) {
        return couponIssueRepository.findById(id)
                .map(CouponIssueResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Coupon issue %d was not found.".formatted(id)));
    }

    private static BusinessException alreadyIssued(String code, long memberId) {
        return new BusinessException(ErrorCode.COUPON_ALREADY_ISSUED,
                "Member %d already has coupon %s.".formatted(memberId, code));
    }
}
