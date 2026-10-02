package practice.j07.ex03;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Objects;

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
 *       0.5 KRW. Creating money never rounds silently: only {@link #multiply} and {@link #allocate} round, as their
 *       rules say. Zeros at the end don't count: 12.500 USD is fine and becomes 12.50.</li>
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
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        int decimals = currency.getDefaultFractionDigits();
        if (decimals < 0) {
            // Pseudo-currencies such as gold (XAU) have no smallest unit, so "exactly as many decimals" means nothing.
            throw new IllegalArgumentException("currency " + currency + " has no defined number of decimals");
        }
        // stripTrailingZeros() removes zeros that carry no value, so 12.500 counts as 2 decimals, not 3.
        if (amount.stripTrailingZeros().scale() > decimals) {
            throw new IllegalArgumentException(
                    amount.toPlainString() + " has more decimals than " + currency + " uses (" + decimals + ")");
        }
        // Setting the scale is what makes the record's generated equals() and hashCode() right. BigDecimal.equals
        // compares the scale, too (12.5 isn't equal to 12.50), but after this line both are 12.50.
        // UNNECESSARY means "never round": the check above guarantees that no rounding is needed.
        amount = amount.setScale(decimals, RoundingMode.UNNECESSARY);
    }

    /**
     * Shortcut for tests and examples: {@code Money.of("12.50", "USD")}.
     *
     * @throws IllegalArgumentException for text that isn't a number (a {@link NumberFormatException}) or an unknown
     *                                  currency code, and for the reasons the constructor gives
     */
    public static Money of(String amount, String currencyCode) {
        // new BigDecimal(String) is exact. There's deliberately no factory that takes a double: new BigDecimal(0.1)
        // is 0.1000000000000000055511151231257827021181583404541015625.
        return new Money(new BigDecimal(amount), Currency.getInstance(currencyCode));
    }

    /** Returns this amount plus {@code other}, which must have the same currency. */
    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    /** Returns this amount minus {@code other}, which must have the same currency. */
    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    /**
     * Returns this amount times {@code factor}, rounded to the currency's decimals with
     * {@link RoundingMode#HALF_EVEN}. For example, the 10% VAT on a price:
     * {@code price.multiply(new BigDecimal("0.1"))}.
     */
    public Money multiply(BigDecimal factor) {
        Objects.requireNonNull(factor, "factor");
        // HALF_EVEN ("banker's rounding") sends an exact half to the even neighbor: 0.025 → 0.02, 0.035 → 0.04.
        // Over many roundings the errors cancel out, while HALF_UP would always push the total up.
        BigDecimal product = amount.multiply(factor)
                .setScale(currency.getDefaultFractionDigits(), RoundingMode.HALF_EVEN);
        return new Money(product, currency);
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
        if (parts < 1) {
            throw new IllegalArgumentException("parts must be at least 1, but was " + parts);
        }
        // Count in the currency's smallest unit (cents, won), where all the arithmetic is on whole numbers.
        // The amount's scale always equals the currency's decimals, so 10.00 USD has the unscaled value 1000.
        BigInteger[] quotientAndRemainder = amount.unscaledValue().divideAndRemainder(BigInteger.valueOf(parts));
        BigInteger base = quotientAndRemainder[0]; // rounded toward zero
        int leftover = quotientAndRemainder[1].intValue(); // same sign as the amount, and |leftover| < parts
        BigInteger oneUnit = BigInteger.valueOf(Integer.signum(leftover));

        List<Money> result = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            result.add(new Money(new BigDecimal(base, amount.scale()), currency));
        }
        return List.copyOf(result);
    }

    /** Returns the currency code and the amount, for example {@code "USD 12.50"}. */
    @Override
    public String toString() {
        // toPlainString() never uses an exponent, while toString() can print a value such as 1E+3.
        return currency.getCurrencyCode() + " " + amount.toPlainString();
    }

    private void requireSameCurrency(Money other) {
        Objects.requireNonNull(other, "other");
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("can't combine " + currency + " with " + other.currency);
        }
    }
}
