package com.example.bookstore.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link CouponIssue}s. A member has each coupon at most once ({@code uk_coupon_issues_coupon_member}). */
public interface CouponIssueRepository extends JpaRepository<CouponIssue, Long> {

    boolean existsByCouponIdAndMemberId(Long couponId, Long memberId);

    Optional<CouponIssue> findByCouponIdAndMemberId(Long couponId, Long memberId);
}
