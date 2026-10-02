package com.example.bookstore.discount;

import org.springframework.stereotype.Component;

/** The discount of {@code GOLD} members: 10% of the amount, rounded down to whole won. */
@Component("goldDiscount")
public class GoldDiscount extends PercentageDiscount {

    public GoldDiscount() {
        super(10);
    }
}
