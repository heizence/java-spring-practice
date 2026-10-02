package practice.j01.ex06;

/**
 * The last step of an order in the bookstore: computes the total and builds the confirmation text.
 *
 * <p>This code is correct; you don't need to change it. When the calculation fails, it wraps the exception in an
 * {@link IllegalStateException} that names the order and keeps the original exception as its <em>cause</em>.
 * Frameworks such as Spring do the same all the time, which is why stack traces often have several
 * "Caused by:" sections.
 */
public final class Checkout {

    private Checkout() {
    }

    /**
     * Returns the confirmation for an order, for example {@code "Order A-1001: total 45.00"}.
     *
     * @param couponCode the coupon the customer typed, or {@code null} if they didn't enter one
     * @throws IllegalStateException if the total can't be computed
     */
    public static String confirm(String orderId, InvoiceLine[] lines, String couponCode) {
        try {
            long total = InvoiceCalculator.totalCents(lines, couponCode);
            return "Order %s: total %d.%02d".formatted(orderId, total / 100, total % 100);
        } catch (RuntimeException e) {
            throw new IllegalStateException("Checkout failed for order " + orderId, e);
        }
    }
}
