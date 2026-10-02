package practice.j05.ex05;

import java.util.Map;

/**
 * Unit prices that depend on the quantity ordered, such as "1 to 9 items: 5.00 each, 10 to 49 items: 4.50 each,
 * 50 or more: 4.00 each".
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The constructor takes the tiers as a map from the first quantity of each tier to its unit price in cents.
 *       The example above is {@code Map.of(1, 500L, 10, 450L, 50, 400L)}.</li>
 *   <li>The lowest tier must start at quantity 1, so that every quantity has a price, and no price may be
 *       negative. Otherwise the constructor throws {@link IllegalArgumentException}. An empty map has no tier
 *       that starts at 1, so it is rejected, too. A {@code null} map, quantity or price throws
 *       {@link NullPointerException}.</li>
 *   <li>The pricing keeps its own copy of the tiers: changing the map after the constructor has no effect.</li>
 *   <li>{@link #unitPriceFor} returns the price of the tier with the highest start that is less than or equal
 *       to the quantity. A quantity below 1 throws {@link IllegalArgumentException}.</li>
 *   <li>Use a {@link java.util.TreeMap} and its {@code floorEntry} method.</li>
 * </ul>
 */
public final class TieredPricing {

    /** Creates the pricing from a map of first quantity → unit price in cents. */
    public TieredPricing(Map<Integer, Long> unitPriceByFirstQuantity) {
        // TODO: J05-05
        throw new UnsupportedOperationException("TODO: J05-05");
    }

    /** The unit price in cents for an order of {@code quantity} items. */
    public long unitPriceFor(int quantity) {
        // TODO: J05-05
        throw new UnsupportedOperationException("TODO: J05-05");
    }
}
