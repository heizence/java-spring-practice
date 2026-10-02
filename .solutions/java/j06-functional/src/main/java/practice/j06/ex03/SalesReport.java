package practice.j06.ex03;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.flatMapping;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.summingLong;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

/**
 * Sales figures for a list of orders. Write each method as one stream pipeline that ends in a collector.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The constructor copies the list: later changes to the caller's list don't change the report. A {@code null}
 *       list, or a {@code null} order in it, throws {@link NullPointerException}.</li>
 *   <li>{@link Status#CANCELLED CANCELLED} orders appear only in {@link #orderCountByStatus()}. Every other method
 *       leaves them out, because they earn nothing.</li>
 *   <li>Every method builds a new result on each call, and never returns {@code null}. A report without orders that
 *       count gives empty maps, and {@link #partitionByTotal} gives two empty lists.</li>
 * </ul>
 */
public final class SalesReport {

    // The biggest spender: the most money, and for equal amounts the name that comes first. Collections.max() picks
    // the greatest entry, so for equal amounts the reversed key order makes the alphabetically first name greatest.
    private static final Comparator<Map.Entry<String, Long>> BIGGEST_SPENDER =
            Map.Entry.<String, Long>comparingByValue()
                    .thenComparing(Map.Entry.comparingByKey(Comparator.reverseOrder()));

    private final List<Order> orders;

    public SalesReport(List<Order> orders) {
        // List.copyOf rejects null (the list and its elements) and protects the report from later changes.
        this.orders = List.copyOf(orders);
    }

    /**
     * Step 1. The revenue of each category in cents: the sum of quantity × unit price over all lines of that
     * category. Categories are in alphabetical order.
     */
    public SortedMap<String, Long> revenueByCategory() {
        // groupingBy(classifier, mapFactory, downstream): TreeMap::new keeps the categories sorted, and
        // summingLong adds up each group instead of collecting it into a list.
        return activeLines().collect(groupingBy(OrderLine::category, TreeMap::new, summingLong(OrderLine::totalCents)));
    }

    /**
     * Step 2. How many orders have each status, cancelled ones included, as an {@link java.util.EnumMap}: statuses
     * come in the order of the {@link Status} enum. Statuses without orders are left out.
     */
    public Map<Status, Long> orderCountByStatus() {
        // An EnumMap is a compact array indexed by the enum's ordinal, and iterates in declaration order.
        // It needs the enum class, so the factory is a lambda rather than EnumMap::new.
        return orders.stream()
                .collect(groupingBy(Order::status, () -> new EnumMap<>(Status.class), counting()));
    }

    /**
     * Step 3. The ids of the orders, split by total: under {@code true}, the orders whose total is at least
     * {@code thresholdCents}; under {@code false}, the others. Both keys are always there, even when a list is empty.
     * The ids keep the order of the orders.
     *
     * @throws IllegalArgumentException if {@code thresholdCents} is less than 1
     */
    public Map<Boolean, List<String>> partitionByTotal(long thresholdCents) {
        if (thresholdCents < 1) {
            throw new IllegalArgumentException("thresholdCents must be at least 1, but was " + thresholdCents);
        }
        // partitioningBy, unlike groupingBy(predicate), always has both keys. mapping() turns each order into its
        // id before collecting.
        return activeOrders()
                .collect(partitioningBy(order -> order.totalCents() >= thresholdCents, mapping(Order::id, toList())));
    }

    /**
     * Step 4. The products each customer bought, each product once. Customers are in alphabetical order, and so are
     * the products in each set.
     */
    public SortedMap<String, Set<String>> productsByCustomer() {
        // flatMapping is the downstream version of flatMap: one order gives several products.
        // toCollection(TreeSet::new) removes duplicates and sorts.
        return activeOrders()
                .collect(groupingBy(Order::customer, TreeMap::new,
                        flatMapping(order -> order.lines().stream().map(OrderLine::product),
                                toCollection(TreeSet::new))));
    }

    /**
     * Step 5. How much each customer spent in cents, over all their orders. Customers are in the order of their first
     * order in the list.
     */
    public SequencedMap<String, Long> totalByCustomer() {
        // Without the merge function (Long::sum), toMap throws IllegalStateException on the second order of the
        // same customer. LinkedHashMap::new keeps the order in which customers first appear.
        return activeOrders()
                .collect(toMap(Order::customer, Order::totalCents, Long::sum, LinkedHashMap::new));
    }

    /**
     * Step 6. The revenue by category as CSV text: the header line {@code category,revenue_cents}, then one line per
     * category in alphabetical order, such as {@code books,11200}. Lines are separated by {@code "\n"}, and there is
     * no line break at the end. Without orders that count, the result is the header line alone.
     */
    public String revenueCsv() {
        Stream<String> rows = revenueByCategory().entrySet().stream()
                .map(entry -> entry.getKey() + "," + entry.getValue());
        // The header is a line of its own, so it goes into the stream. As joining()'s prefix, it would leave a
        // line break after the header when there are no rows.
        return Stream.concat(Stream.of("category,revenue_cents"), rows).collect(joining("\n"));
    }

    /**
     * Step 7. For each category, the customer who spent the most on it: the sum of their line totals in that
     * category, over all their orders. If customers spent the same amount, the name that comes first alphabetically
     * wins. Categories are in alphabetical order.
     */
    public SortedMap<String, String> topCustomerByCategory() {
        // A local record: a named pair that exists only inside this method. It keeps the customer next to each
        // line after flatMap, which would otherwise lose the order the line came from.
        record Purchase(String category, String customer, long cents) {
        }
        return activeOrders()
                .flatMap(order -> order.lines().stream()
                        .map(line -> new Purchase(line.category(), order.customer(), line.totalCents())))
                .collect(groupingBy(Purchase::category, TreeMap::new,
                        // For each category: first the amount per customer, then the biggest spender of that map.
                        collectingAndThen(groupingBy(Purchase::customer, summingLong(Purchase::cents)),
                                spent -> Collections.max(spent.entrySet(), BIGGEST_SPENDER).getKey())));
    }

    private Stream<Order> activeOrders() {
        return orders.stream().filter(order -> order.status() != Status.CANCELLED);
    }

    private Stream<OrderLine> activeLines() {
        return activeOrders().flatMap(order -> order.lines().stream());
    }
}
