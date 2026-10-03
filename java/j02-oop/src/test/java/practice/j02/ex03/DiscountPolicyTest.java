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
        @DisplayName("a discount of 0 leaves the price as it is")
        void zeroDiscount() {
            assertThat(new FixedDiscount(0).apply(10_000)).isEqualTo(10_000);
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
        @DisplayName("a fixed discount larger than the price makes the price 0, not negative")
        void fixedDiscountLargerThanPrice() {
            assertThat(new FixedDiscount(5_000).apply(3_000)).isZero();
        }

        @Test
        @DisplayName("a price of 0 stays 0")
        void zeroPrice() {
            assertThat(new FixedDiscount(1_000).apply(0)).isZero();
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
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "price {0}")
        @ValueSource(longs = {-1, Long.MIN_VALUE})
        @DisplayName("rejects a negative price, whatever the subclass")
        void negativePrice(long price) {
            assertThatThrownBy(() -> new FixedDiscount(1_000).apply(price))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(price));
            assertThatThrownBy(() -> alwaysTakingOff(0).apply(price))
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
    }
}
