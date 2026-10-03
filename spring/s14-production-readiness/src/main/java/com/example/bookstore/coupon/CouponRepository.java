package com.example.bookstore.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Coupon}s. Codes are unique ({@code uk_coupons_code}). */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);
}
