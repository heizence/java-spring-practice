package practice.j06.ex02;

import static java.util.function.Predicate.not;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

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

    // Highest total first.
    private static final Comparator<Order> HIGHEST_TOTAL_FIRST =
            Comparator.comparingLong(Order::totalCents).reversed();

    private OrderQueries() {
    }

    /** The orders with this status, in their original order. */
    public static List<Order> withStatus(List<Order> orders, OrderStatus status) {
        // Without this check, a null status would quietly match nothing.
        Objects.requireNonNull(status, "status");
        return orders.stream()
                .filter(order -> order.status() == status) // enum constants are compared with ==
                .toList(); // unmodifiable, unlike collect(Collectors.toList())
    }

    /**
     * The {@code n} orders with the highest totals, highest first. Orders with the same total are sorted by id, so
     * the result doesn't depend on the order of the input. With fewer than {@code n} orders, you get all of them.
     *
     * @throws IllegalArgumentException if {@code n} is negative, with {@code n} in its message
     */
    public static List<Order> topByTotal(List<Order> orders, int n) {
        if (n < 0) {
            throw new IllegalArgumentException("n must not be negative, but was " + n);
        }
        // sorted() sorts a copy inside the stream. List.sort() would change the caller's list instead.
        return orders.stream()
                .sorted(HIGHEST_TOTAL_FIRST)
                .limit(n)
                .toList();
    }

    /** Each customer's name once, in alphabetical order. */
    public static List<String> customers(List<Order> orders) {
        return orders.stream()
                .map(Order::customer)
                .distinct()
                .sorted()
                .toList();
    }

    /** Whether at least one order has a total <em>greater than</em> {@code thresholdCents}. False for no orders. */
    public static boolean hasOrderOver(List<Order> orders, long thresholdCents) {
        // anyMatch stops at the first match, and is false for an empty stream.
        return orders.stream().anyMatch(order -> order.totalCents() > thresholdCents);
    }

    /** The sum of the totals of all orders that aren't cancelled. 0 for no orders. */
    public static long revenueCents(List<Order> orders) {
        // mapToLong gives a LongStream, which can sum() without boxing every value into a Long.
        return orders.stream()
                .filter(not(OrderQueries::isCancelled))
                .mapToLong(Order::totalCents)
                .sum();
    }

    /**
     * The average total of the orders that aren't cancelled, in cents, with the fraction: totals of 1 and 2 cents
     * give 1.5. Returns 0.0 when no order counts.
     */
    public static double averageOrderValue(List<Order> orders) {
        // average() returns an OptionalDouble, because an empty stream has no average. The method's contract
        // turns "no orders" into 0.0.
        return orders.stream()
                .filter(not(OrderQueries::isCancelled))
                .mapToLong(Order::totalCents)
                .average()
                .orElse(0.0);
    }

    private static boolean isCancelled(Order order) {
        return order.status() == OrderStatus.CANCELLED;
    }
}
