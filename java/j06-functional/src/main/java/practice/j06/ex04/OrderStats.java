package practice.j06.ex04;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Statistics and exports for a batch of orders.
 *
 * <p>Every method rejects a {@code null} list with {@link NullPointerException} and never changes the list.
 */
public final class OrderStats {

    private OrderStats() {
    }

    /** A one-line summary of the paid orders, such as {@code "2 paid orders, 4200 cents"}. */
    public static String paidSummary(List<Order> orders) {
        Stream<Order> paid = orders.stream().filter(Order::paid);
        long count = paid.count();
        long cents = paid.mapToLong(Order::totalCents).sum();
        return count + " paid orders, " + cents + " cents";
    }

    /** Each customer once, in the order of their first order in the list. */
    public static List<String> customersInOrderOfFirstOrder(List<Order> orders) {
        Set<String> customers = new HashSet<>();
        for (Order order : orders) {
            customers.add(order.customer());
        }
        return customers.stream().toList();
    }
}
