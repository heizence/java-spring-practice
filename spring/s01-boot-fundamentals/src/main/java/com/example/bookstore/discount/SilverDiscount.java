package com.example.bookstore.discount;

import org.springframework.stereotype.Component;

/** The discount of {@code SILVER} members: 5% of the amount, rounded down to whole won. */
@Component("silverDiscount")
public class SilverDiscount extends PercentageDiscount {

    public SilverDiscount() {
        super(5);
    }
}
