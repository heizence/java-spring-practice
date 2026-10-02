package practice.j02.ex07;

import java.util.Objects;

import practice.j02.ex07.PaymentMethod.BankTransfer;
import practice.j02.ex07.PaymentMethod.Card;
import practice.j02.ex07.PaymentMethod.Points;

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

    static final long TRANSFER_FEE = 500;
    static final long FREE_TRANSFER_FROM = 100_000;

    private FeeCalculator() {
    }

    /** The fee for paying {@code amount} won with {@code method}. */
    public static long fee(PaymentMethod method, long amount) {
        Objects.requireNonNull(method, "method");
        if (amount < 1) {
            throw new IllegalArgumentException("amount must be at least 1 won, but was " + amount);
        }
        // No default branch: PaymentMethod is sealed, so the compiler knows these cases cover every payment
        // method. If someone adds a fourth kind, this switch stops compiling until it handles the new kind.
        // Cases are tried from top to bottom, so a guarded case ("when ...") must come before the unguarded case
        // for the same type; the other order doesn't compile.
        return switch (method) {
            case Card(_, boolean international) when international -> percentOf(amount, 3);
            case Card _ -> percentOf(amount, 2);
            case BankTransfer _ when amount > FREE_TRANSFER_FROM -> 0;
            case BankTransfer _ -> TRANSFER_FEE;
            case Points(long balance) when balance < amount -> throw new IllegalArgumentException(
                    "not enough points: balance " + balance + ", amount " + amount);
            case Points _ -> 0;
        };
    }

    private static long percentOf(long amount, int percent) {
        return Math.multiplyExact(amount, percent) / 100; // rounds down, and fails loudly on overflow
    }
}
