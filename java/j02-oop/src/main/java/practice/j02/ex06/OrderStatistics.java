package practice.j02.ex06;

import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
        Objects.requireNonNull(statuses, "statuses");
        // EnumMap keeps its keys in declaration order and stores the values in an array indexed by ordinal(),
        // which is smaller and faster than a HashMap.
        var counts = new EnumMap<OrderStatus, Integer>(OrderStatus.class);
        for (OrderStatus status : OrderStatus.values()) {
            counts.put(status, 0);
        }
        for (OrderStatus status : statuses) {
            counts.merge(Objects.requireNonNull(status, "status"), 1, Integer::sum);
        }
        // Map.copyOf would lose the order, so wrap the EnumMap instead. Nobody else holds a reference to it, so
        // this read-only view is as good as a copy.
        return Collections.unmodifiableMap(counts);
    }
}
