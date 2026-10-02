package practice.j06.ex04;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
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

    /** One shipping label per order, such as {@code "A-1:mina"}, in the same order as the orders. */
    public static List<String> shippingLabels(List<Order> orders) {
        List<String> labels = new ArrayList<>();
        orders.parallelStream() // batches can be large: use every CPU core
                .map(order -> order.id() + ":" + order.customer())
                .forEach(labels::add);
        return labels;
    }

    /** Writes the id of every order to the audit log, in order, and returns the number of orders. */
    public static long countAndAudit(List<Order> orders, Consumer<String> auditLog) {
        return orders.stream()
                .peek(order -> auditLog.accept(order.id()))
                .count();
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
