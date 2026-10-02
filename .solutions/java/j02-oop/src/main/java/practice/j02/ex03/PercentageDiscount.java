package practice.j02.ex03;

/**
 * Takes a percentage of the price off, rounded down to whole won: 10% off 999 won takes 99 won off.
 * {@link DiscountPolicy} lists the rules of {@code apply}.
 */
public final class PercentageDiscount extends DiscountPolicy {

    private final int percent;

    /**
     * A discount of {@code percent} percent. {@code percent} must be from 0 to 100, inclusive; otherwise throws
     * {@link IllegalArgumentException} with the percentage in its message.
     */
    public PercentageDiscount(int percent) {
        if (percent < 0 || percent > 100) {
            throw new IllegalArgumentException("percent must be between 0 and 100, but was " + percent);
        }
        this.percent = percent;
    }

    /**
     * {@code percent} percent of {@code price}, rounded down. If price × percent doesn't fit in a {@code long},
     * throws {@link ArithmeticException} instead of returning a wrong number.
     */
    @Override
    protected long discount(long price) {
        // Integer division rounds down for numbers that aren't negative. multiplyExact fails loudly on overflow;
        // a plain * would wrap around and silently produce a nonsense discount.
        return Math.multiplyExact(price, percent) / 100;
    }
}
