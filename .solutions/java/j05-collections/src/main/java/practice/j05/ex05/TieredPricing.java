package practice.j05.ex05;

import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;
import java.util.TreeMap;

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

    private final NavigableMap<Integer, Long> unitPriceByFirstQuantity;

    /** Creates the pricing from a map of first quantity → unit price in cents. */
    public TieredPricing(Map<Integer, Long> unitPriceByFirstQuantity) {
        Objects.requireNonNull(unitPriceByFirstQuantity, "unitPriceByFirstQuantity");
        // Copy first, then validate the copy. The copy also sorts the tiers, and it protects us from
        // later changes to the caller's map. (new TreeMap<>(map) throws NullPointerException for a null key.)
        var tiers = new TreeMap<>(unitPriceByFirstQuantity);
        if (tiers.isEmpty() || tiers.firstKey() != 1) {
            throw new IllegalArgumentException("the lowest tier must start at quantity 1, but the tiers are " + tiers);
        }
        for (var tier : tiers.entrySet()) {
            Long price = Objects.requireNonNull(tier.getValue(), "price");
            if (price < 0) {
                throw new IllegalArgumentException(
                        "price must not be negative, but the tier at " + tier.getKey() + " costs " + price);
            }
        }
        this.unitPriceByFirstQuantity = tiers;
    }

    /** The unit price in cents for an order of {@code quantity} items. */
    public long unitPriceFor(int quantity) {
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1, but was " + quantity);
        }
        // floorEntry finds the tier with the greatest start <= quantity in O(log n). It is never null here,
        // because the constructor made sure that a tier starts at 1.
        return unitPriceByFirstQuantity.floorEntry(quantity).getValue();
    }
}
