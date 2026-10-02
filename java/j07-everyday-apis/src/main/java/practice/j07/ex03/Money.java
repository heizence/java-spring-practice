package practice.j07.ex03;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.List;

/**
 * An amount of money in one currency, as an immutable value.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The amount always has exactly as many decimals as the currency uses
 *       ({@link Currency#getDefaultFractionDigits()}): 2 for USD and EUR, 0 for KRW and JPY, 3 for BHD. The
 *       constructor adds zeros where needed: {@code new Money(new BigDecimal("12.5"), usd).amount()} is
 *       {@code 12.50}.</li>
 *   <li>An amount with more decimals than the currency uses throws {@link IllegalArgumentException}: 0.005 USD or
 *       0.5 KRW. Creating money never rounds silently; only {@link #multiply} rounds. Zeros at the end don't count:
 *       12.500 USD is fine and becomes 12.50.</li>
 *   <li>A currency without a defined number of decimals, such as gold ({@code XAU}), throws
 *       {@link IllegalArgumentException}.</li>
 *   <li>Amounts may be zero or negative, for example for refunds.</li>
 *   <li>{@link #add} and {@link #subtract} need two amounts in the same currency. Otherwise they throw
 *       {@link IllegalArgumentException} with both currency codes in the message.</li>
 *   <li>{@link #multiply} rounds the result to the currency's decimals with {@link RoundingMode#HALF_EVEN}.</li>
 *   <li>{@link #allocate} splits the money into parts that always add up to exactly the original amount.</li>
 *   <li>Two {@code Money} values are equal when they have the same currency and the same amount, so 12.5 USD and
 *       12.50 USD are equal and have the same hash code.</li>
 *   <li>{@link #toString()} is the currency code, a space and the amount written out without an exponent:
 *       {@code "USD 12.50"}, {@code "KRW -1000"}.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}.</li>
 * </ul>
 *
 * @param amount   the amount, with exactly as many decimals as the currency uses
 * @param currency the currency
 */
public record Money(BigDecimal amount, Currency currency) {

    /** Checks the arguments and gives the amount exactly as many decimals as the currency uses. */
    public Money {
        // TODO: J07-03
        throw new UnsupportedOperationException("TODO: J07-03");
    }

    /**
     * Shortcut for tests and examples: {@code Money.of("12.50", "USD")}.
     *
     * @throws IllegalArgumentException for text that isn't a number (a {@link NumberFormatException}) or an unknown
     *                                  currency code, and for the reasons the constructor gives
     */
    public static Money of(String amount, String currencyCode) {
        // TODO: J07-03
        throw new UnsupportedOperationException("TODO: J07-03");
    }

    /** Returns this amount plus {@code other}, which must have the same currency. */
    public Money add(Money other) {
        // TODO: J07-03
        throw new UnsupportedOperationException("TODO: J07-03");
    }

    /** Returns this amount minus {@code other}, which must have the same currency. */
    public Money subtract(Money other) {
        // TODO: J07-03
        throw new UnsupportedOperationException("TODO: J07-03");
    }

    /**
     * Returns this amount times {@code factor}, rounded to the currency's decimals with
     * {@link RoundingMode#HALF_EVEN}. For example, the 10% VAT on a price:
     * {@code price.multiply(new BigDecimal("0.1"))}.
     */
    public Money multiply(BigDecimal factor) {
        // TODO: J07-03
        throw new UnsupportedOperationException("TODO: J07-03");
    }

    /**
     * Splits this money into {@code parts} amounts that add up to exactly this amount, for example to split a bill.
     *
     * <p>Every part starts as this amount divided by {@code parts}, rounded toward zero to the currency's decimals.
     * The smallest units that are left over (cents, won) go one each to the first parts. So 10.00 USD in 3 parts is
     * [3.34, 3.33, 3.33], 100 KRW in 3 parts is [34, 33, 33], and -10.00 USD in 3 parts is [-3.34, -3.33, -3.33].
     *
     * @return an unmodifiable list with {@code parts} elements
     * @throws IllegalArgumentException if {@code parts} is less than 1
     */
    public List<Money> allocate(int parts) {
        // TODO: J07-03
        throw new UnsupportedOperationException("TODO: J07-03");
    }

    /** Returns the currency code and the amount, for example {@code "USD 12.50"}. */
    @Override
    public String toString() {
        // TODO: J07-03
        throw new UnsupportedOperationException("TODO: J07-03");
    }
}
