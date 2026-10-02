package practice.j06.ex04;

import java.util.Objects;

/**
 * An order in a batch. Given code: it has no bugs.
 *
 * @param id         the order id, such as {@code "A-1"}
 * @param customer   the customer's name
 * @param paid       whether the customer has paid
 * @param totalCents the order total in cents, at least 0
 */
public record Order(String id, String customer, boolean paid, long totalCents) {

    public Order {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(customer, "customer");
        if (totalCents < 0) {
            throw new IllegalArgumentException("totalCents must not be negative, but was " + totalCents);
        }
    }
}
