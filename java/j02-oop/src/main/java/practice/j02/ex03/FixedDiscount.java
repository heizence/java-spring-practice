package practice.j02.ex03;

/** Takes a fixed amount off, for example 3,000 won. {@link DiscountPolicy} lists the rules of {@code apply}. */
public final class FixedDiscount extends DiscountPolicy {

    private final long amount;

    /**
     * A discount of {@code amount} won; 0 is allowed. A negative amount throws {@link IllegalArgumentException} with
     * the amount in its message.
     */
    public FixedDiscount(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must not be negative, but was " + amount);
        }
        this.amount = amount;
    }

    /** The same amount for every price. */
    @Override
    protected long discount(long price) {
        return amount; // may be more than the price: apply() clamps it
    }
}
