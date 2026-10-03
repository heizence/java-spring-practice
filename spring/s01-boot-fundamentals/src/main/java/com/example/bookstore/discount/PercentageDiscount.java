package com.example.bookstore.discount;

/** A discount of a fixed percentage of the amount, rounded down to whole won. */
abstract class PercentageDiscount implements DiscountPolicy {

    private final int percent;

    PercentageDiscount(int percent) {
        this.percent = percent;
    }

    @Override
    public long discountFor(long amount) {
        // Integer division rounds down, so the customer never gets a fraction of a won more than the rate allows.
        return amount * percent / 100;
    }
}
