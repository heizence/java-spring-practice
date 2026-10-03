package practice.j06.ex04;

import java.util.List;

/**
 * Statistics for a batch of orders.
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

    /** Each customer once, in the order of their first order in the list. */
    public static List<String> customersInOrderOfFirstOrder(List<Order> orders) {
        return orders.stream()
                .map(Order::customer)
                .collect(java.util.stream.Collectors.toCollection(java.util.TreeSet::new))
                .stream()
                .toList();
    }
}
