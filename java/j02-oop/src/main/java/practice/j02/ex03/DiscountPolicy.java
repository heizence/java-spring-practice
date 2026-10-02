package practice.j02.ex03;

/**
 * A rule that lowers a price, such as "3,000 won off" or "10% off". Prices are whole won.
 *
 * <p>This class uses the <em>template method</em> pattern: {@link #apply} is {@code final} and holds the rules that
 * every discount must follow. Subclasses only decide how much to take off, in {@link #discount}.
 *
 * <h2>Rules of {@link #apply}</h2>
 * <ul>
 *   <li>A negative price throws {@link IllegalArgumentException} with the price in the message.</li>
 *   <li>The discount comes from {@link #discount} and is clamped to the range from 0 to the price: a discount can
 *       never make a price negative, and a negative discount can never raise it.</li>
 *   <li>The result is the price minus the clamped discount.</li>
 * </ul>
 */
public abstract class DiscountPolicy {

    /** The price after the discount: never negative and never more than {@code price}. */
    public final long apply(long price) {
        // TODO: J02-03
        throw new UnsupportedOperationException("TODO: J02-03");
    }

    /**
     * How many won to take off {@code price}, which is never negative. An implementation may return a number
     * outside the range from 0 to the price: {@link #apply} clamps it.
     */
    protected abstract long discount(long price);
}
