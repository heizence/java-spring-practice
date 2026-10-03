package com.example.bookstore.discount;

import org.springframework.stereotype.Component;

/**
 * The discount of {@code BASIC} members: none.
 *
 * <p>It's also the bookstore's default policy: code that asks for "a {@link DiscountPolicy}" without saying which one
 * gets this one. Guests without an account pay the full price too.
 */
// TODO: S01-03 — make this the DiscountPolicy that Spring injects when nobody says which one (see the README).
@Component("basicDiscount")
public class BasicDiscount implements DiscountPolicy {

    @Override
    public long discountFor(long amount) {
        return 0;
    }
}
