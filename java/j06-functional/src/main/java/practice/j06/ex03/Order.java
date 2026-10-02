package practice.j06.ex03;

import java.util.List;
import java.util.Objects;

/**
 * An order with one or more lines. Given code: you don't need to change it.
 *
 * @param id       the order id, such as {@code "O-1"}
 * @param customer the customer's name
 * @param status   where the order is in its life cycle
 * @param lines    what was ordered: at least one line, stored as an unmodifiable copy
 */
public record Order(String id, String customer, Status status, List<OrderLine> lines) {

    public Order {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(status, "status");
        lines = List.copyOf(lines);
        if (lines.isEmpty()) {
            throw new IllegalArgumentException("order " + id + " has no lines");
        }
    }

    /** The sum of the line totals. */
    public long totalCents() {
        return lines.stream().mapToLong(OrderLine::totalCents).sum();
    }
}
