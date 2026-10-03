package com.example.bookstore.coupon;

import java.net.URI;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Coupons for members: {@code POST /api/coupons/{code}/issues} and {@code GET /api/coupon-issues/{id}}. */
@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    /** Gives a member one copy of a coupon: {@code 201 Created} with the issue and its {@code Location}. */
    @PostMapping("/api/coupons/{code}/issues")
    public ResponseEntity<CouponIssueResponse> issue(@PathVariable String code,
                                                     @Valid @RequestBody IssueCouponRequest request) {
        CouponIssueResponse issue = couponService.issue(code, request.memberId());
        return ResponseEntity.created(URI.create("/api/coupon-issues/" + issue.id())).body(issue);
    }

    @GetMapping("/api/coupon-issues/{id}")
    public CouponIssueResponse getIssue(@PathVariable long id) {
        return couponService.findIssue(id);
    }
}
