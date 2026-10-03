package com.example.bookstore.discount;

import org.springframework.stereotype.Component;

/**
 * The discount of the bookstore's own employees: 20% of the amount, rounded down to whole won. It belongs to no member
 * grade: staff purchases always use it.
 */
@Component("staffDiscount")
public class StaffDiscount extends PercentageDiscount {

    public StaffDiscount() {
        super(20);
    }
}
