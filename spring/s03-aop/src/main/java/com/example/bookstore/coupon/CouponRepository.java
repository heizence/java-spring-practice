package com.example.bookstore.coupon;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.springframework.stereotype.Repository;

/**
 * Stores coupons and the copies given to members, in memory.
 *
 * <p>S03 has no database, so this class stands in for the {@code coupons} and {@code coupon_issues} tables, and
 * starts with the coupons of the seed data ({@code spring/BOOKSTORE.md}): {@code WELCOME5000} (issued to members
 * 1–15), {@code FLASH10000} (10 copies, none issued, valid in October 2026), {@code AUTUMN3000} (issued to members
 * 1–10) and {@code EXPIRED2000} (valid in July and August 2026, issued to members 2–6). From S06 on, a Spring Data
 * repository replaces it.
 *
 * <p>The bean is shared by every request thread, so its methods are {@code synchronized}.
 */
@Repository
public class CouponRepository {

    private final Map<String, Coupon> couponsByCode = new HashMap<>();
    private final Map<IssueKey, CouponIssue> issues = new HashMap<>();

    public CouponRepository() {
        loadSeedData();
    }

    public synchronized Optional<Coupon> findByCode(String code) {
        return Optional.ofNullable(couponsByCode.get(code));
    }

    public synchronized boolean existsIssue(String couponCode, long memberId) {
        return issues.containsKey(new IssueKey(couponCode, memberId));
    }

    public synchronized Optional<CouponIssue> findIssue(String couponCode, long memberId) {
        return Optional.ofNullable(issues.get(new IssueKey(couponCode, memberId)));
    }

    /**
     * Saves a new issue.
     *
     * @throws IllegalStateException if the member already has this coupon. The database's unique key
     *                               {@code uk_coupon_issues_coupon_member} plays this role from S06 on.
     */
    public synchronized void saveIssue(CouponIssue issue) {
        Objects.requireNonNull(issue, "issue");
        CouponIssue earlier = issues.putIfAbsent(new IssueKey(issue.couponCode(), issue.memberId()), issue);
        if (earlier != null) {
            throw new IllegalStateException("Member %d already has coupon %s."
                    .formatted(issue.memberId(), issue.couponCode()));
        }
    }

    /**
     * Throws away every change and loads the seed data again. Tests call it before each test, so that they don't
     * depend on each other; from S05 on, they restore the database instead.
     */
    public synchronized void restoreSeedData() {
        loadSeedData();
    }

    private void loadSeedData() {
        couponsByCode.clear();
        issues.clear();
        addCoupon("WELCOME5000", 5_000, 1_000, "2026-01-01T00:00:00Z", "2026-12-31T23:59:59Z", 1, 15, 7);
        addCoupon("FLASH10000", 10_000, 10, "2026-10-01T00:00:00Z", "2026-10-31T23:59:59Z", 1, 0, 10);
        addCoupon("AUTUMN3000", 3_000, 500, "2026-09-01T00:00:00Z", "2026-11-30T23:59:59Z", 1, 10, 9);
        addCoupon("EXPIRED2000", 2_000, 100, "2026-07-01T00:00:00Z", "2026-08-31T23:59:59Z", 2, 6, 7);
    }

    /**
     * Adds a coupon that members {@code firstMember} to {@code lastMember} already received, one per day in
     * {@code month} of 2026 at 03:00 UTC, as in the seed data.
     */
    private void addCoupon(String code, long discountAmount, int totalQuantity, String validFrom, String validUntil,
                           long firstMember, long lastMember, int month) {
        int issued = (int) Math.max(0, lastMember - firstMember + 1);
        couponsByCode.put(code, new Coupon(code, discountAmount, totalQuantity, issued, Instant.parse(validFrom),
                Instant.parse(validUntil)));
        for (long memberId = firstMember; memberId <= lastMember; memberId++) {
            Instant issuedAt = LocalDate.of(2026, month, 1).plusDays(memberId).atTime(LocalTime.of(3, 0))
                    .toInstant(ZoneOffset.UTC);
            issues.put(new IssueKey(code, memberId), new CouponIssue(code, memberId, issuedAt));
        }
    }

    private record IssueKey(String couponCode, long memberId) {
    }
}
