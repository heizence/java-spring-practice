package practice.j01.ex06;

import java.util.Objects;

/**
 * One line of an invoice: which book, how many copies, and the price of one copy in cents.
 *
 * <p>This is a <em>record</em>: a class whose only job is to hold data. The line above the braces declares the
 * fields, a constructor, and the accessors {@code title()}, {@code quantity()} and {@code unitPriceCents()}.
 * J02 covers records properly. This code is correct; you don't need to change it.
 *
 * @param title          the book title
 * @param quantity       how many copies, at least 1
 * @param unitPriceCents the price of one copy in cents, not negative
 */
public record InvoiceLine(String title, int quantity, long unitPriceCents) {

    /** Checks the values before the record is created. */
    public InvoiceLine {
        Objects.requireNonNull(title, "title");
        if (quantity < 1) {
            throw new IllegalArgumentException("quantity must be at least 1, but was " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("unitPriceCents must not be negative, but was " + unitPriceCents);
        }
    }
}
