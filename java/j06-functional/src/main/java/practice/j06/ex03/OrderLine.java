package practice.j06.ex03;

import java.util.Objects;

/**
 * One line of an order: a product, how many, and the price of one. Given code: you don't need to change it.
 *
 * @param product        the product name, such as {@code "Clean Code"}
 * @param category       the product's category, such as {@code "books"}
 * @param quantity       how many, at least 1
 * @param unitPriceCents the price of one, in cents, at least 0
 */
public record OrderLine(String product, String category, int quantity, long unitPriceCents) {

    public OrderLine {
        Objects.requireNonNull(product, "product");
        Objects.requireNonNull(category, "category");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1, but was " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("unitPriceCents must not be negative, but was " + unitPriceCents);
        }
    }

    /** Quantity × unit price. Throws {@link ArithmeticException} instead of overflowing. */
    public long totalCents() {
        return Math.multiplyExact(quantity, unitPriceCents);
    }
}
