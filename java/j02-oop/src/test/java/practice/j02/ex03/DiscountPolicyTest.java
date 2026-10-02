package practice.j02.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J02-03 · Interfaces and polymorphism")
class DiscountPolicyTest {

    /** A test-only subclass whose discount() returns a fixed number, even a nonsense one. */
    private static DiscountPolicy alwaysTakingOff(long discount) {
        // An anonymous class: a subclass without a name, defined and instantiated in one expression.
        return new DiscountPolicy() {
            @Override
            protected long discount(long price) {
                return discount;
            }
        };
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("FixedDiscount takes its amount off the price")
        void fixedDiscount() {
            var policy = new FixedDiscount(3_000);

            assertThat(policy.apply(10_000)).isEqualTo(7_000);
        }

        @Test
        @DisplayName("PercentageDiscount takes its percentage off the price")
        void percentageDiscount() {
            var policy = new PercentageDiscount(10);

            assertThat(policy.apply(25_000)).isEqualTo(22_500);
        }

        @Test
        @DisplayName("a discount of 0 leaves the price as it is")
        void zeroDiscount() {
            assertThat(new FixedDiscount(0).apply(10_000)).isEqualTo(10_000);
            assertThat(new PercentageDiscount(0).apply(10_000)).isEqualTo(10_000);
        }

        @Test
        @DisplayName("100% off makes the price 0")
        void hundredPercent() {
            assertThat(new PercentageDiscount(100).apply(10_000)).isZero();
        }

        @Test
        @DisplayName("apply() asks the subclass for the discount and takes it off")
        void templateMethodCallsHook() {
            DiscountPolicy halfPrice = new DiscountPolicy() {
                @Override
                protected long discount(long price) {
                    return price / 2;
                }
            };

            assertThat(halfPrice.apply(10_000)).isEqualTo(5_000);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("PercentageDiscount rounds the discount down to whole won")
        void percentageRoundsDown() {
            assertThat(new PercentageDiscount(10).apply(999)).isEqualTo(900); // 99.9 won off becomes 99
            assertThat(new PercentageDiscount(15).apply(1_001)).isEqualTo(851); // 150.15 won off becomes 150
        }

        @Test
        @DisplayName("a fixed discount larger than the price makes the price 0, not negative")
        void fixedDiscountLargerThanPrice() {
            assertThat(new FixedDiscount(5_000).apply(3_000)).isZero();
        }

        @Test
        @DisplayName("a price of 0 stays 0")
        void zeroPrice() {
            assertThat(new FixedDiscount(1_000).apply(0)).isZero();
            assertThat(new PercentageDiscount(50).apply(0)).isZero();
        }

        @Test
        @DisplayName("a subclass that returns a negative discount can't raise the price")
        void negativeDiscountIsClamped() {
            var policy = alwaysTakingOff(-500);

            assertThat(policy.apply(10_000)).isEqualTo(10_000);
        }

        @Test
        @DisplayName("a subclass that returns more than the price can't make it negative")
        void hugeDiscountIsClamped() {
            var policy = alwaysTakingOff(Long.MAX_VALUE);

            assertThat(policy.apply(10_000)).isZero();
        }

        @Test
        @DisplayName("PercentageDiscount works for a large price that doesn't overflow")
        void largePrice() {
            var policy = new PercentageDiscount(10);

            assertThat(policy.apply(900_000_000_000_000_000L)).isEqualTo(810_000_000_000_000_000L);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "price {0}")
        @ValueSource(longs = {-1, Long.MIN_VALUE})
        @DisplayName("rejects a negative price")
        void negativePrice(long price) {
            assertThatThrownBy(() -> new FixedDiscount(1_000).apply(price))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(price));
            assertThatThrownBy(() -> new PercentageDiscount(10).apply(price))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {-1, Long.MIN_VALUE})
        @DisplayName("FixedDiscount rejects a negative amount")
        void negativeAmount(long amount) {
            assertThatThrownBy(() -> new FixedDiscount(amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(amount));
        }

        @ParameterizedTest(name = "percent {0}")
        @ValueSource(ints = {-1, 101, Integer.MIN_VALUE, Integer.MAX_VALUE})
        @DisplayName("PercentageDiscount rejects a percentage below 0 or above 100")
        void percentOutOfRange(int percent) {
            assertThatThrownBy(() -> new PercentageDiscount(percent))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(percent));
        }

        @Test
        @DisplayName("PercentageDiscount throws ArithmeticException instead of overflowing")
        void percentageOverflow() {
            var policy = new PercentageDiscount(50);

            assertThatThrownBy(() -> policy.apply(Long.MAX_VALUE / 10)).isInstanceOf(ArithmeticException.class);
        }
    }
}
