package practice.j02.ex06;

import java.util.Set;

/**
 * Where an order is in its life cycle.
 *
 * <pre>
 * PLACED ──► PAID ──► SHIPPED ──► DELIVERED
 *    │         │
 *    └─────────┴──► CANCELLED
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>These moves are allowed, and no others: {@code PLACED → PAID}, {@code PLACED → CANCELLED},
 *       {@code PAID → SHIPPED}, {@code PAID → CANCELLED} and {@code SHIPPED → DELIVERED}. {@code DELIVERED} and
 *       {@code CANCELLED} are final: nothing comes after them. No status can move to itself.</li>
 *   <li>{@link #allowedNext()} returns the statuses this one can move to, as a set that can't be modified.</li>
 *   <li>{@link #transitionTo} returns {@code next} when the move is allowed. Otherwise it throws
 *       {@link IllegalStateException} with both statuses in the message.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}.</li>
 * </ul>
 */
public enum OrderStatus {
    PLACED,
    PAID,
    SHIPPED,
    DELIVERED,
    CANCELLED;

    /** The statuses this one can move to. Empty for a final status. */
    public Set<OrderStatus> allowedNext() {
        // TODO: J02-06
        throw new UnsupportedOperationException("TODO: J02-06");
    }

    /** {@code true} if an order with this status may move to {@code next}. */
    public boolean canTransitionTo(OrderStatus next) {
        // TODO: J02-06
        throw new UnsupportedOperationException("TODO: J02-06");
    }

    /** Returns {@code next} if the move is allowed. */
    public OrderStatus transitionTo(OrderStatus next) {
        // TODO: J02-06
        throw new UnsupportedOperationException("TODO: J02-06");
    }
}
