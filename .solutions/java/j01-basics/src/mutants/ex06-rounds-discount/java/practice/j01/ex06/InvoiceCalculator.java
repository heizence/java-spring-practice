package practice.j01.ex06;

import java.util.Locale;

/**
 * Calculates invoice totals for the bookstore checkout. All amounts are in cents, stored in a {@code long}, so
 * there are no floating-point rounding errors.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The subtotal is the sum of {@code quantity × unitPriceCents} over all lines. An invoice without lines
 *       has a subtotal of 0.</li>
 *   <li>A coupon code gives a percentage off the subtotal: {@code WELCOME10} gives 10 %, {@code BOOKWORM25}
 *       gives 25 %. Customers type the codes by hand, so spaces around a code and letter case don't matter:
 *       {@code " welcome10 "} works, too.</li>
 *   <li>A {@code null} or blank coupon code means the customer didn't enter one: no discount. An unknown code
 *       gives no discount either.</li>
 *   <li>The discount is rounded down to whole cents: 10 % of 1995 cents is 199 cents.</li>
 *   <li>The total is the subtotal minus the discount.</li>
 *   <li>A {@code null} array of lines throws {@link NullPointerException}.</li>
 * </ul>
 */
public final class InvoiceCalculator {

    private InvoiceCalculator() {
    }

    /** Returns the sum of {@code quantity × unitPriceCents} over all lines. */
    public static long subtotalCents(InvoiceLine[] lines) {
        long subtotal = 0;
        // Bug 1 was "i < lines.length - 1", which skipped the last line. A for-each loop can't get the bounds
        // wrong, so prefer it whenever you don't need the index.
        for (InvoiceLine line : lines) {
            subtotal += line.quantity() * line.unitPriceCents();
        }
        return subtotal;
    }

    /** Returns the discount in percent for a coupon code: 10, 25, or 0 for no coupon or an unknown one. */
    public static int discountPercent(String couponCode) {
        // Bug 3: a customer without a coupon sends null, and couponCode.strip() threw NullPointerException.
        // Handle "no coupon" first, then normalize what the customer typed.
        if (couponCode == null) {
            return 0;
        }
        String code = couponCode.strip().toUpperCase(Locale.ROOT);
        return switch (code) {
            case "WELCOME10" -> 10;
            case "BOOKWORM25" -> 25;
            default -> 0; // also covers a blank code, which is "" after strip()
        };
    }

    /** Returns {@code percent} percent of {@code subtotalCents}, rounded down to whole cents. */
    public static long discountCents(long subtotalCents, int percent) {
        return Math.round(subtotalCents * percent / 100.0);
    }

    /** Returns the subtotal of {@code lines} minus the discount for {@code couponCode}. */
    public static long totalCents(InvoiceLine[] lines, String couponCode) {
        long subtotal = subtotalCents(lines);
        int percent = discountPercent(couponCode);
        return subtotal - discountCents(subtotal, percent);
    }
}
