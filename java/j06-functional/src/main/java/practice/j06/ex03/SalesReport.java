package practice.j06.ex03;

import java.util.List;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.SortedMap;

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

    public SalesReport(List<Order> orders) {
        // TODO: J06-03
        throw new UnsupportedOperationException("TODO: J06-03");
    }

    /**
     * Step 1. The revenue of each category in cents: the sum of quantity × unit price over all lines of that
     * category. Categories are in alphabetical order.
     */
    public SortedMap<String, Long> revenueByCategory() {
        // TODO: J06-03
        throw new UnsupportedOperationException("TODO: J06-03");
    }

    /**
     * Step 2. How many orders have each status, cancelled ones included, as an {@link java.util.EnumMap}: statuses
     * come in the order of the {@link Status} enum. Statuses without orders are left out.
     */
    public Map<Status, Long> orderCountByStatus() {
        // TODO: J06-03
        throw new UnsupportedOperationException("TODO: J06-03");
    }

    /**
     * Step 3. The ids of the orders, split by total: under {@code true}, the orders whose total is at least
     * {@code thresholdCents}; under {@code false}, the others. Both keys are always there, even when a list is empty.
     * The ids keep the order of the orders.
     *
     * @throws IllegalArgumentException if {@code thresholdCents} is less than 1
     */
    public Map<Boolean, List<String>> partitionByTotal(long thresholdCents) {
        // TODO: J06-03
        throw new UnsupportedOperationException("TODO: J06-03");
    }

    /**
     * Step 4. The products each customer bought, each product once. Customers are in alphabetical order, and so are
     * the products in each set.
     */
    public SortedMap<String, Set<String>> productsByCustomer() {
        // TODO: J06-03
        throw new UnsupportedOperationException("TODO: J06-03");
    }

    /**
     * Step 5. How much each customer spent in cents, over all their orders. Customers are in the order of their first
     * order in the list.
     */
    public SequencedMap<String, Long> totalByCustomer() {
        // TODO: J06-03
        throw new UnsupportedOperationException("TODO: J06-03");
    }

    /**
     * Step 6. The revenue by category as CSV text: the header line {@code category,revenue_cents}, then one line per
     * category in alphabetical order, such as {@code books,11200}. Lines are separated by {@code "\n"}, and there is
     * no line break at the end. Without orders that count, the result is the header line alone.
     */
    public String revenueCsv() {
        // TODO: J06-03
        throw new UnsupportedOperationException("TODO: J06-03");
    }

    /**
     * Step 7. For each category, the customer who spent the most on it: the sum of their line totals in that
     * category, over all their orders. If customers spent the same amount, the name that comes first alphabetically
     * wins. Categories are in alphabetical order.
     */
    public SortedMap<String, String> topCustomerByCategory() {
        // TODO: J06-03
        throw new UnsupportedOperationException("TODO: J06-03");
    }
}
