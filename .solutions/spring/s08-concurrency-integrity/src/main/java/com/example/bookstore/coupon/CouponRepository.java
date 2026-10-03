package com.example.bookstore.coupon;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Coupon}s. Codes are unique ({@code uk_coupons_code}). */
public interface CouponRepository extends JpaRepository<Coupon, Long> {

    Optional<Coupon> findByCode(String code);

    /**
     * Counts one more issued copy of a coupon, if a copy is left, with one {@code UPDATE} statement: the database
     * checks and changes the count while it holds the row's lock. Increases the version like an entity update would.
     * Entities that this transaction already loaded keep their old values.
     *
     * @return 1 if a copy was taken, 0 if none was left
     */
    @Modifying
    @Query("""
            update Coupon c set c.issuedQuantity = c.issuedQuantity + 1, c.version = c.version + 1
            where c.id = :couponId and c.issuedQuantity < c.totalQuantity""")
    int takeCopy(long couponId);
}
