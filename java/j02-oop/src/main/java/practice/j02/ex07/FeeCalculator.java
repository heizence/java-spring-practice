package practice.j02.ex07;

/**
 * Works out the fee the shop pays for a payment, in whole won.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Card: 2% of the amount; 3% for an international card. Rounded down to whole won.</li>
 *   <li>Bank transfer: 500 won, or nothing (0) for an amount of 100,000 won or more.</li>
 *   <li>Points: no fee (0). But the points must cover the whole amount: if the balance is smaller than the amount,
 *       throws {@link IllegalArgumentException} with both numbers in its message.</li>
 *   <li>The amount must be at least 1 won: {@link IllegalArgumentException} with the amount in its message. A
 *       {@code null} payment method throws {@link NullPointerException}.</li>
 *   <li>A fee computation that doesn't fit in a {@code long} throws {@link ArithmeticException}.</li>
 * </ul>
 */
public final class FeeCalculator {

    private FeeCalculator() {
    }

    /** The fee for paying {@code amount} won with {@code method}. */
    public static long fee(PaymentMethod method, long amount) {
        // TODO: J02-07
        throw new UnsupportedOperationException("TODO: J02-07");
    }
}
