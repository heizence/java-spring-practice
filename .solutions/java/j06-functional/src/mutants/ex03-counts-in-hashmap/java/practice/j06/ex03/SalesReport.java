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
 *       count gives empty maps.</li>
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

    public SortedMap<String, Long> revenueByCategory() {
        // groupingBy(classifier, mapFactory, downstream): TreeMap::new keeps the categories sorted, and
        // summingLong adds up each group instead of collecting it into a list.
        return activeLines().collect(groupingBy(OrderLine::category, TreeMap::new, summingLong(OrderLine::totalCents)));
    }

    public Map<Status, Long> orderCountByStatus() {
        return orders.stream()
                .collect(groupingBy(Order::status, counting()));
    }

    public Map<Boolean, List<String>> partitionByTotal(long thresholdCents) {
        if (thresholdCents < 1) {
            throw new IllegalArgumentException("thresholdCents must be at least 1, but was " + thresholdCents);
        }
        // partitioningBy, unlike groupingBy(predicate), always has both keys. mapping() turns each order into its
        // id before collecting.
        return activeOrders()
                .collect(partitioningBy(order -> order.totalCents() >= thresholdCents, mapping(Order::id, toList())));
    }

    public SortedMap<String, Set<String>> productsByCustomer() {
        // flatMapping is the downstream version of flatMap: one order gives several products.
        // toCollection(TreeSet::new) removes duplicates and sorts.
        return activeOrders()
                .collect(groupingBy(Order::customer, TreeMap::new,
                        flatMapping(order -> order.lines().stream().map(OrderLine::product),
                                toCollection(TreeSet::new))));
    }

    public SequencedMap<String, Long> totalByCustomer() {
        // Without the merge function (Long::sum), toMap throws IllegalStateException on the second order of the
        // same customer. LinkedHashMap::new keeps the order in which customers first appear.
        return activeOrders()
                .collect(toMap(Order::customer, Order::totalCents, Long::sum, LinkedHashMap::new));
    }

    public String revenueCsv() {
        Stream<String> rows = revenueByCategory().entrySet().stream()
                .map(entry -> entry.getKey() + "," + entry.getValue());
        // The header is a line of its own, so it goes into the stream. As joining()'s prefix, it would leave a
        // line break after the header when there are no rows.
        return Stream.concat(Stream.of("category,revenue_cents"), rows).collect(joining("\n"));
    }

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
