package practice.j07.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J07-03 · Money with BigDecimal")
class MoneyTest {

    private static final Currency USD = Currency.getInstance("USD");
    private static final Currency KRW = Currency.getInstance("KRW");

    private static Money usd(String amount) {
        return Money.of(amount, "USD");
    }

    private static Money krw(String amount) {
        return Money.of(amount, "KRW");
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("stores the amount with exactly the currency's number of decimals")
        void normalizesScale() {
            assertThat(new Money(new BigDecimal("12.5"), USD).amount()).isEqualTo(new BigDecimal("12.50"));
            assertThat(new Money(new BigDecimal("13500"), KRW).amount()).isEqualTo(new BigDecimal("13500"));
        }

        @Test
        @DisplayName("of() reads the amount and the currency code from text")
        void ofReadsText() {
            Money money = Money.of("12.50", "USD");

            assertThat(money.amount()).isEqualTo(new BigDecimal("12.50"));
            assertThat(money.currency()).isEqualTo(USD);
        }

        @Test
        @DisplayName("adds two amounts")
        void adds() {
            assertThat(usd("10.25").add(usd("0.75"))).isEqualTo(usd("11.00"));
        }

        @Test
        @DisplayName("subtracts, also below zero")
        void subtracts() {
            assertThat(krw("1000").subtract(krw("1500"))).isEqualTo(krw("-500"));
        }

        @Test
        @DisplayName("multiplies and rounds to the currency's decimals")
        void multiplies() {
            assertThat(usd("19.99").multiply(new BigDecimal("0.1"))).isEqualTo(usd("2.00"));
        }

        @Test
        @DisplayName("allocates 10.00 USD in 3 parts as 3.34, 3.33 and 3.33")
        void allocates() {
            assertThat(usd("10.00").allocate(3)).containsExactly(usd("3.34"), usd("3.33"), usd("3.33"));
        }

        @Test
        @DisplayName("toString() shows the currency code and the amount")
        void readableToString() {
            assertThat(usd("12.5")).hasToString("USD 12.50");
            assertThat(krw("13500")).hasToString("KRW 13500");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @ParameterizedTest(name = "{1} {0} × {2} = {3} {0}", quoteTextArguments = false)
        @CsvSource({
                "USD, 0.05, 0.5, 0.02",
                "USD, 0.07, 0.5, 0.04",
                "USD, -0.05, 0.5, -0.02",
                "KRW, 5, 0.5, 2",
                "KRW, 7, 0.5, 4",
                "KRW, 1025, 0.1, 102"
        })
        @DisplayName("multiply() rounds a result exactly halfway to the even neighbor (HALF_EVEN)")
        void roundsHalfToEven(String currency, String amount, String factor, String expected) {
            Money result = Money.of(amount, currency).multiply(new BigDecimal(factor));

            assertThat(result).isEqualTo(Money.of(expected, currency));
        }

        @ParameterizedTest(name = "{1} {0} × {2} = {3} {0}", quoteTextArguments = false)
        @CsvSource({
                "USD, 1.00, 0.826, 0.83",
                "USD, 1.00, 0.824, 0.82",
                "USD, -1.00, 0.826, -0.83",
                "KRW, 13500, 1.1, 14850",
                "USD, 10.00, 0, 0.00",
                "USD, 10.00, -1, -10.00"
        })
        @DisplayName("multiply() rounds every other result to the nearest value")
        void roundsToNearest(String currency, String amount, String factor, String expected) {
            Money result = Money.of(amount, currency).multiply(new BigDecimal(factor));

            assertThat(result).isEqualTo(Money.of(expected, currency));
        }

        @Test
        @DisplayName("accepts zeros beyond the currency's decimals and drops them")
        void acceptsTrailingZeros() {
            assertThat(usd("12.500").amount()).isEqualTo(new BigDecimal("12.50"));
            assertThat(krw("1000.00").amount()).isEqualTo(new BigDecimal("1000"));
        }

        @Test
        @DisplayName("works with any number of decimals: the Bahraini dinar (BHD) has 3")
        void threeDecimalCurrency() {
            assertThat(Money.of("1.2", "BHD").amount()).isEqualTo(new BigDecimal("1.200"));
            assertThat(Money.of("1.234", "BHD").amount()).isEqualTo(new BigDecimal("1.234"));
        }

        @Test
        @DisplayName("12.5 USD and 12.50 USD are equal and have the same hash code")
        void equalityIgnoresScale() {
            Money a = new Money(new BigDecimal("12.5"), USD);
            Money b = new Money(new BigDecimal("12.50"), USD);

            assertThat(a).isEqualTo(b);
            assertThat(a.hashCode()).isEqualTo(b.hashCode());
            assertThat(new HashSet<>(List.of(a, b))).hasSize(1);
        }

        @Test
        @DisplayName("the same number in another currency isn't equal")
        void differentCurrencyNotEqual() {
            assertThat(krw("100")).isNotEqualTo(Money.of("100", "JPY"));
        }

        @Test
        @DisplayName("allows zero and negative amounts")
        void zeroAndNegative() {
            assertThat(usd("0")).hasToString("USD 0.00");
            assertThat(usd("-5")).hasToString("USD -5.00");
        }

        @Test
        @DisplayName("writes the amount without an exponent")
        void noExponent() {
            assertThat(new Money(new BigDecimal("1E+3"), KRW)).hasToString("KRW 1000");
        }

        @Test
        @DisplayName("allocates 100 KRW in 3 parts as 34, 33 and 33")
        void allocatesWon() {
            assertThat(krw("100").allocate(3)).containsExactly(krw("34"), krw("33"), krw("33"));
        }

        @Test
        @DisplayName("allocates a negative amount like a positive one")
        void allocatesNegative() {
            assertThat(usd("-10.00").allocate(3)).containsExactly(usd("-3.34"), usd("-3.33"), usd("-3.33"));
        }

        @Test
        @DisplayName("gives a single leftover cent to the first part")
        void allocatesOneCent() {
            assertThat(usd("0.01").allocate(3)).containsExactly(usd("0.01"), usd("0.00"), usd("0.00"));
        }

        @Test
        @DisplayName("allocating into 1 part returns the whole amount")
        void allocatesIntoOnePart() {
            assertThat(usd("10.00").allocate(1)).containsExactly(usd("10.00"));
        }

        @Test
        @DisplayName("allocate() returns a list that can't be modified")
        void allocateIsUnmodifiable() {
            List<Money> parts = usd("10.00").allocate(2);

            assertThatThrownBy(() -> parts.add(usd("1.00"))).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("operations return new values and leave the original unchanged")
        void immutable() {
            Money price = usd("10.00");

            price.add(usd("5.00"));
            price.multiply(new BigDecimal("3"));

            assertThat(price).isEqualTo(usd("10.00"));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "{1} {0}", quoteTextArguments = false)
        @CsvSource({
                "USD, 0.005",
                "USD, 12.345",
                "KRW, 0.5",
                "JPY, 100.1",
                "BHD, 1.2345"
        })
        @DisplayName("rejects more decimals than the currency uses, instead of rounding")
        void rejectsTooManyDecimals(String currency, String amount) {
            assertThatThrownBy(() -> Money.of(amount, currency)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a currency without a defined number of decimals, such as gold (XAU)")
        void rejectsCurrencyWithoutDecimals() {
            assertThatThrownBy(() -> new Money(BigDecimal.ONE, Currency.getInstance("XAU")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("add() and subtract() reject another currency, naming both")
        void rejectsCurrencyMismatch() {
            assertThatThrownBy(() -> usd("1.00").add(krw("1000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("USD")
                    .hasMessageContaining("KRW");
            assertThatThrownBy(() -> usd("1.00").subtract(krw("1000")))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "{0} parts")
        @ValueSource(ints = {0, -1})
        @DisplayName("allocate() rejects fewer than 1 part")
        void rejectsTooFewParts(int parts) {
            assertThatThrownBy(() -> usd("10.00").allocate(parts)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("of() rejects text that isn't a number, and unknown currency codes")
        void ofRejectsBadText() {
            assertThatThrownBy(() -> Money.of("12,50", "USD")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Money.of("12.50", "XYZ")).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null arguments")
        void rejectsNulls() {
            assertThatThrownBy(() -> new Money(null, USD)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Money(BigDecimal.ONE, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> usd("1.00").add(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> usd("1.00").multiply(null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("allocated parts always add up to the original and differ by at most one smallest unit")
        void allocatePartsAddUp() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            List<String> currencies = List.of("USD", "KRW", "JPY", "BHD");

            for (int step = 0; step < 2_000; step++) {
                Currency currency = Currency.getInstance(currencies.get(random.nextInt(currencies.size())));
                int decimals = currency.getDefaultFractionDigits();
                long smallestUnits = random.nextLong(-1_000_000_000L, 1_000_000_001L);
                var money = new Money(BigDecimal.valueOf(smallestUnits, decimals), currency);
                int parts = 1 + random.nextInt(20);

                List<Money> allocated = money.allocate(parts);

                String context = "%s in %d parts at step %d".formatted(money, parts, step);
                assertThat(allocated).as(context).hasSize(parts);
                assertThat(allocated.stream().reduce(Money::add).orElseThrow()).as("sum of " + context)
                        .isEqualTo(money);
                List<BigDecimal> sizes = allocated.stream().map(part -> part.amount().abs()).toList();
                assertThat(sizes).as("larger parts first: " + context).isSortedAccordingTo(Comparator.reverseOrder());
                BigDecimal spread = sizes.getFirst().subtract(sizes.getLast()).movePointRight(decimals);
                assertThat(spread).as("spread of " + context).isLessThanOrEqualTo(BigDecimal.ONE);
            }
        }
    }
}
