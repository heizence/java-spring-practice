package practice.j06.ex02;

import java.util.List;

/**
 * Everyday questions about a list of orders. Answer each one with a single stream pipeline.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code null} list or status throws {@link NullPointerException}. The list you get is never modified.</li>
 *   <li>Returned lists are unmodifiable and never {@code null}.</li>
 *   <li>Only {@link #revenueCents} and {@link #averageOrderValue} leave out {@link OrderStatus#CANCELLED CANCELLED}
 *       orders, because a cancelled order earns nothing. The other methods look at every order.</li>
 * </ul>
 */
public final class OrderQueries {

    private OrderQueries() {
    }

    /** The orders with this status, in their original order. */
    public static List<Order> withStatus(List<Order> orders, OrderStatus status) {
        // TODO: J06-02
        throw new UnsupportedOperationException("TODO: J06-02");
    }

    /**
     * The {@code n} orders with the highest totals, highest first. Orders with the same total are sorted by id, so
     * the result doesn't depend on the order of the input. With fewer than {@code n} orders, you get all of them.
     *
     * @throws IllegalArgumentException if {@code n} is negative, with {@code n} in its message
     */
    public static List<Order> topByTotal(List<Order> orders, int n) {
        // TODO: J06-02
        throw new UnsupportedOperationException("TODO: J06-02");
    }

    /** Each customer's name once, in alphabetical order. */
    public static List<String> customers(List<Order> orders) {
        // TODO: J06-02
        throw new UnsupportedOperationException("TODO: J06-02");
    }

    /** Whether at least one order has a total <em>greater than</em> {@code thresholdCents}. False for no orders. */
    public static boolean hasOrderOver(List<Order> orders, long thresholdCents) {
        // TODO: J06-02
        throw new UnsupportedOperationException("TODO: J06-02");
    }

    /** The sum of the totals of all orders that aren't cancelled. 0 for no orders. */
    public static long revenueCents(List<Order> orders) {
        // TODO: J06-02
        throw new UnsupportedOperationException("TODO: J06-02");
    }

    /**
     * The average total of the orders that aren't cancelled, in cents, with the fraction: totals of 1 and 2 cents
     * give 1.5. Returns 0.0 when no order counts.
     */
    public static double averageOrderValue(List<Order> orders) {
        // TODO: J06-02
        throw new UnsupportedOperationException("TODO: J06-02");
    }
}
