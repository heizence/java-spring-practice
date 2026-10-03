package com.example.bookstore.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Coupon}s. Codes are unique ({@code uk_coupons_code}). */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    /**
     * Counts one more issued copy of a coupon with one {@code UPDATE} statement. Increases the version like an entity
     * update would.
     *
     * @return the number of rows changed: 1, or 0 if the coupon doesn't exist
     */
    @Modifying
    @Query("""
            update Coupon c set c.issuedQuantity = c.issuedQuantity + 1, c.version = c.version + 1
            where c.id = :couponId""")
    int countIssuedCopy(long couponId);
}
