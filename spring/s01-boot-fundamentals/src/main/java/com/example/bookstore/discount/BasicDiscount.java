package com.example.bookstore.discount;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The discount of {@code BASIC} members: none.
 *
 * <p>It's also the bookstore's default policy: code that asks for "a {@link DiscountPolicy}" without saying which one
 * gets this one. Guests without an account pay the full price too.
 */
// @Primary: of the four DiscountPolicy beans, this one wins wherever an injection point doesn't say which it wants.
// A @Qualifier still beats it, and a Map<String, DiscountPolicy> or List<DiscountPolicy> still gets all four.
@Primary
@Component("basicDiscount")
public class BasicDiscount implements DiscountPolicy {

    @Override
    public long discountFor(long amount) {
        return 0;
    }
}
