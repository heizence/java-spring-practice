package practice.j06.ex02;

import java.util.Objects;

/**
 * An order in the shop. Given code: you don't need to change it.
 *
 * <p>Money is a {@code long} number of cents here, never a {@code double}: binary floating point can't store most
 * decimal fractions exactly (J07 shows {@code BigDecimal}, the other safe choice).
 *
 * @param id         the order id, such as {@code "A-1"}
 * @param customer   the customer's name
 * @param status     where the order is in its life cycle
 * @param totalCents the order total in cents, at least 0
 */
public record Order(String id, String customer, OrderStatus status, long totalCents) {

    public Order {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(status, "status");
        if (totalCents < 0) {
            throw new IllegalArgumentException("totalCents must not be negative, but was " + totalCents);
        }
    }
}
