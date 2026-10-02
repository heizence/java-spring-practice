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

    // Highest total first. .reversed() is called before .thenComparing(), so it reverses only the total; ids
    // still go from A to Z. Ties need a rule, or the result would depend on the order of the input.
    private static final Comparator<Order> HIGHEST_TOTAL_FIRST =
            Comparator.comparingLong(Order::totalCents).reversed().thenComparing(Order::id);

    private OrderQueries() {
    }

    public static List<Order> withStatus(List<Order> orders, OrderStatus status) {
        // Without this check, a null status would quietly match nothing.
        Objects.requireNonNull(status, "status");
        return orders.stream()
                .filter(order -> order.status() == status) // enum constants are compared with ==
                .toList(); // unmodifiable, unlike collect(Collectors.toList())
    }

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

    public static List<String> customers(List<Order> orders) {
        return orders.stream()
                .map(Order::customer)
                .distinct()
                .sorted()
                .toList();
    }

    public static boolean hasOrderOver(List<Order> orders, long thresholdCents) {
        // anyMatch stops at the first match, and is false for an empty stream.
        return orders.stream().anyMatch(order -> order.totalCents() > thresholdCents);
    }

    public static long revenueCents(List<Order> orders) {
        // mapToLong gives a LongStream, which can sum() without boxing every value into a Long.
        return orders.stream()
                .filter(not(OrderQueries::isCancelled))
                .mapToLong(Order::totalCents)
                .sum();
    }

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
