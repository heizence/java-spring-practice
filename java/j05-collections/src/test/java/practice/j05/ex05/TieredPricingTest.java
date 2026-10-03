package practice.j05.ex05;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J05-05 · Binary search tree")
class TieredPricingTest {

    /** 1 to 9 items: 5.00 each, 10 to 49 items: 4.50 each, 50 or more: 4.00 each. */
    private static final Map<Integer, Long> TIERS = Map.of(1, 500L, 10, 450L, 50, 400L);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @ParameterizedTest(name = "{0} items cost {1} cents each", quoteTextArguments = false)
        @CsvSource({"1, 500", "9, 500", "10, 450", "49, 450", "50, 400", "1000, 400"})
        @DisplayName("returns the unit price of the tier that the quantity falls into")
        void priceOfTier(int quantity, long expectedUnitPrice) {
            var pricing = new TieredPricing(TIERS);

            assertThat(pricing.unitPriceFor(quantity)).isEqualTo(expectedUnitPrice);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a single tier gives every quantity the same price")
        void singleTier() {
            var pricing = new TieredPricing(Map.of(1, 250L));

            assertThat(pricing.unitPriceFor(1)).isEqualTo(250);
            assertThat(pricing.unitPriceFor(Integer.MAX_VALUE)).isEqualTo(250);
        }

        @Test
        @DisplayName("the largest possible quantity gets the top tier's price")
        void largestQuantity() {
            var pricing = new TieredPricing(TIERS);

            assertThat(pricing.unitPriceFor(Integer.MAX_VALUE)).isEqualTo(400);
        }

        @Test
        @DisplayName("accepts the tiers in any order")
        void tiersInAnyOrder() {
            Map<Integer, Long> tiers = new LinkedHashMap<>();
            tiers.put(50, 400L);
            tiers.put(1, 500L);
            tiers.put(10, 450L);

            var pricing = new TieredPricing(tiers);

            assertThat(pricing.unitPriceFor(9)).isEqualTo(500);
            assertThat(pricing.unitPriceFor(10)).isEqualTo(450);
            assertThat(pricing.unitPriceFor(50)).isEqualTo(400);
        }

        @Test
        @DisplayName("accepts a price of 0")
        void freeTier() {
            var pricing = new TieredPricing(Map.of(1, 100L, 100, 0L));

            assertThat(pricing.unitPriceFor(100)).isZero();
        }

        @Test
        @DisplayName("keeps its own copy of the tiers: later changes to the map have no effect")
        void defensiveCopy() {
            Map<Integer, Long> tiers = new HashMap<>(TIERS);
            var pricing = new TieredPricing(tiers);

            tiers.put(1, 999L);
            tiers.put(5, 1L);

            assertThat(pricing.unitPriceFor(5)).isEqualTo(500);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "quantity {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("rejects a quantity below 1")
        void rejectsQuantityBelowOne(int quantity) {
            var pricing = new TieredPricing(TIERS);

            assertThatThrownBy(() -> pricing.unitPriceFor(quantity)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects an empty map of tiers")
        void rejectsNoTiers() {
            assertThatThrownBy(() -> new TieredPricing(Map.of())).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "lowest tier starts at {0}")
        @ValueSource(ints = {-5, 0, 2})
        @DisplayName("rejects tiers whose lowest tier doesn't start at quantity 1")
        void rejectsLowestTierNotAtOne(int lowestStart) {
            Map<Integer, Long> tiers = Map.of(lowestStart, 500L, 10, 450L);

            assertThatThrownBy(() -> new TieredPricing(tiers)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a negative price")
        void rejectsNegativePrice() {
            Map<Integer, Long> tiers = Map.of(1, 500L, 10, -1L);

            assertThatThrownBy(() -> new TieredPricing(tiers)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a null map and a null price")
        void rejectsNulls() {
            Map<Integer, Long> withNullPrice = new HashMap<>();
            withNullPrice.put(1, null);

            assertThatThrownBy(() -> new TieredPricing(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new TieredPricing(withNullPrice)).isInstanceOf(NullPointerException.class);
        }
    }
}
