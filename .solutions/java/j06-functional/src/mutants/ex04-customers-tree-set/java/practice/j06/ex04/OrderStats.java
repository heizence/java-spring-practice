package practice.j06.ex04;

import java.util.List;
import java.util.function.Consumer;

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
        // A stream can run only once: the second terminal operation threw IllegalStateException. Keep the data in a
        // list and start a new stream for each question. (Collectors.teeing could also do both in one pass.)
        List<Order> paid = orders.stream().filter(Order::paid).toList();
        long cents = paid.stream().mapToLong(Order::totalCents).sum();
        return paid.size() + " paid orders, " + cents + " cents";
    }

    /** One shipping label per order, such as {@code "A-1:mina"}, in the same order as the orders. */
    public static List<String> shippingLabels(List<Order> orders) {
        // Let the stream build the list. toList() keeps the encounter order and is safe even in a parallel stream,
        // while forEach(labels::add) on a parallel stream added from several threads at once to an ArrayList (which
        // isn't thread-safe) in no particular order. For cheap work like this, sequential is also faster.
        return orders.stream()
                .map(order -> order.id() + ":" + order.customer())
                .toList();
    }

    /** Writes the id of every order to the audit log, in order, and returns the number of orders. */
    public static long countAndAudit(List<Order> orders, Consumer<String> auditLog) {
        // peek() is meant for debugging. Since Java 9, count() skips the whole pipeline when it can get the size
        // from the source, so peek's side effect never ran. A side effect deserves a plain forEach (or a loop).
        orders.forEach(order -> auditLog.accept(order.id()));
        return orders.size();
    }

    /** Each customer once, in the order of their first order in the list. */
    public static List<String> customersInOrderOfFirstOrder(List<Order> orders) {
        return orders.stream()
                .map(Order::customer)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new))
                .stream()
                .toList();
    }
}
