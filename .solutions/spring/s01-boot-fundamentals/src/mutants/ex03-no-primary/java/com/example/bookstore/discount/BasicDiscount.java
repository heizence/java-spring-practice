package com.example.bookstore.discount;

import org.springframework.stereotype.Component;

/** Mutant: BasicDiscount isn't marked @Primary, so "a DiscountPolicy" without a qualifier is ambiguous. */
@Component("basicDiscount")
public class BasicDiscount implements DiscountPolicy {

    @Override
    public long discountFor(long amount) {
        return 0;
    }
}
