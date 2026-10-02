package practice.j02.ex03;

/** Takes a fixed amount off, for example 3,000 won. {@link DiscountPolicy} lists the rules of {@code apply}. */
public final class FixedDiscount extends DiscountPolicy {

    /**
     * A discount of {@code amount} won; 0 is allowed. A negative amount throws {@link IllegalArgumentException} with
     * the amount in its message.
     */
    public FixedDiscount(long amount) {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    /** The same amount for every price. */
    @Override
    protected long discount(long price) {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }
}
