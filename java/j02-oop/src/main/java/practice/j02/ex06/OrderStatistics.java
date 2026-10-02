package practice.j02.ex06;

import java.util.List;
import java.util.Map;

/** Reports about many orders at once. */
public final class OrderStatistics {

    private OrderStatistics() {
    }

    /**
     * Counts how many of {@code statuses} have each status.
     *
     * <ul>
     *   <li>Every status is a key of the result, with 0 if no order has it.</li>
     *   <li>The keys come in the order the statuses are declared in {@link OrderStatus}: {@code PLACED} first.</li>
     *   <li>The result can't be modified.</li>
     *   <li>A {@code null} list or a {@code null} element throws {@link NullPointerException}.</li>
     * </ul>
     */
    public static Map<OrderStatus, Integer> countByStatus(List<OrderStatus> statuses) {
        // TODO: J02-06
        throw new UnsupportedOperationException("TODO: J02-06");
    }
}
