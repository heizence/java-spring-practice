package practice.j05.ex08;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J05-08 · Sorting and binary search")
class SearchTest {

    private static final int[] TENS = {10, 20, 30, 40};

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("lowerBound() returns the index of the first value >= target")
        void lowerBound() {
            assertThat(Search.lowerBound(TENS, 30)).isEqualTo(2);
            assertThat(Search.lowerBound(TENS, 25)).isEqualTo(2);
        }

        @Test
        @DisplayName("upperBound() returns the index of the first value > target")
        void upperBound() {
            assertThat(Search.upperBound(TENS, 30)).isEqualTo(3);
            assertThat(Search.upperBound(TENS, 25)).isEqualTo(2);
        }

        @Test
        @DisplayName("countInRange() counts the values between low and high, both included")
        void countInRange() {
            assertThat(Search.countInRange(TENS, 20, 30)).isEqualTo(2);
            assertThat(Search.countInRange(TENS, 15, 35)).isEqualTo(2);
            assertThat(Search.countInRange(TENS, 10, 40)).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("with duplicates, lowerBound() finds the first one and upperBound() the index after the last")
        void duplicates() {
            int[] sorted = {1, 2, 2, 2, 3};

            assertThat(Search.lowerBound(sorted, 2)).isEqualTo(1);
            assertThat(Search.upperBound(sorted, 2)).isEqualTo(4);
            assertThat(Search.countInRange(sorted, 2, 2)).isEqualTo(3);
        }

        @Test
        @DisplayName("finds the first of many equal values")
        void manyDuplicates() {
            int[] sorted = new int[1_001];
            Arrays.fill(sorted, 1, 1_001, 5); // one 0, then a thousand 5s

            assertThat(Search.lowerBound(sorted, 5)).isEqualTo(1);
            assertThat(Search.upperBound(sorted, 5)).isEqualTo(1_001);
        }

        @Test
        @DisplayName("a target below every value gives index 0")
        void targetBelowAll() {
            assertThat(Search.lowerBound(TENS, 5)).isZero();
            assertThat(Search.upperBound(TENS, 5)).isZero();
        }

        @Test
        @DisplayName("a target above every value gives the length of the array")
        void targetAboveAll() {
            assertThat(Search.lowerBound(TENS, 50)).isEqualTo(4);
            assertThat(Search.upperBound(TENS, 50)).isEqualTo(4);
            assertThat(Search.upperBound(TENS, 40)).isEqualTo(4);
        }

        @Test
        @DisplayName("an empty array gives 0 everywhere")
        void emptyArray() {
            int[] empty = {};

            assertThat(Search.lowerBound(empty, 1)).isZero();
            assertThat(Search.upperBound(empty, 1)).isZero();
            assertThat(Search.countInRange(empty, 0, 10)).isZero();
        }

        @Test
        @DisplayName("an array with one element")
        void singleElement() {
            int[] seven = {7};

            assertThat(Search.lowerBound(seven, 7)).isZero();
            assertThat(Search.upperBound(seven, 7)).isEqualTo(1);
            assertThat(Search.lowerBound(seven, 8)).isEqualTo(1);
            assertThat(Search.upperBound(seven, 6)).isZero();
        }

        @Test
        @DisplayName("handles negative values and the extreme int values without overflowing")
        void extremeValues() {
            int[] sorted = {Integer.MIN_VALUE, -5, 0, 5, Integer.MAX_VALUE};

            assertThat(Search.lowerBound(sorted, Integer.MIN_VALUE)).isZero();
            assertThat(Search.upperBound(sorted, Integer.MIN_VALUE)).isEqualTo(1);
            assertThat(Search.lowerBound(sorted, Integer.MAX_VALUE)).isEqualTo(4);
            assertThat(Search.upperBound(sorted, Integer.MAX_VALUE)).isEqualTo(5);
            assertThat(Search.countInRange(sorted, Integer.MIN_VALUE, Integer.MAX_VALUE)).isEqualTo(5);
            assertThat(Search.countInRange(sorted, Integer.MAX_VALUE, Integer.MAX_VALUE)).isEqualTo(1);
            assertThat(Search.countInRange(sorted, -5, 5)).isEqualTo(3);
        }

        @Test
        @DisplayName("countInRange() returns 0 when low is greater than high")
        void lowGreaterThanHigh() {
            assertThat(Search.countInRange(new int[] {1, 2, 3}, 3, 1)).isZero();
        }

        @Test
        @DisplayName("countInRange() returns 0 when no value lies in the range")
        void emptyRange() {
            assertThat(Search.countInRange(new int[] {1, 2, 3, 10}, 4, 9)).isZero();
        }

        @ParameterizedTest(name = "target {0}")
        @ValueSource(ints = {5, 15, 25, 35})
        @DisplayName("for a missing value, lowerBound() is the insertion point that Arrays.binarySearch encodes")
        void matchesBinarySearchInsertionPoint(int target) {
            int[] sorted = {10, 20, 30};
            int binarySearchResult = Arrays.binarySearch(sorted, target); // -(insertion point) - 1

            assertThat(Search.lowerBound(sorted, target)).isEqualTo(-binarySearchResult - 1);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS) // binary search needs well under a second for this
        @DisplayName("answers 100,000 lookups in 1,000,000 values within 2 seconds, which a loop over all values can't")
        void largeInputIsFast() {
            int[] evens = new int[1_000_000];
            for (int i = 0; i < evens.length; i++) {
                evens[i] = 2 * i;
            }

            for (int i = 0; i < 100_000; i++) {
                int target = 20 * i + 1; // odd, so never in the array, and spread over the whole array
                assertThat(Search.lowerBound(evens, target)).isEqualTo(10 * i + 1);
            }
            assertThat(Search.countInRange(evens, 0, 1_999_998)).isEqualTo(1_000_000);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a null array")
        void rejectsNull() {
            assertThatThrownBy(() -> Search.lowerBound(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Search.upperBound(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Search.countInRange(null, 1, 2)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("matches a simple loop over every value, for 1,000 random sorted arrays")
        void matchesLinearScan() {
            var random = new Random(42); // fixed seed: a failure is reproducible

            for (int trial = 0; trial < 1_000; trial++) {
                int[] sorted = new int[random.nextInt(30)];
                for (int i = 0; i < sorted.length; i++) {
                    sorted[i] = random.nextInt(21) - 10; // small range: many duplicates
                }
                Arrays.sort(sorted);
                int target = random.nextInt(25) - 12;
                int low = random.nextInt(25) - 12;
                int high = random.nextInt(25) - 12;

                int firstAtLeast = 0;
                while (firstAtLeast < sorted.length && sorted[firstAtLeast] < target) {
                    firstAtLeast++;
                }
                int firstAbove = 0;
                while (firstAbove < sorted.length && sorted[firstAbove] <= target) {
                    firstAbove++;
                }
                int inRange = 0;
                for (int value : sorted) {
                    if (low <= value && value <= high) {
                        inRange++;
                    }
                }

                String array = Arrays.toString(sorted);
                assertThat(Search.lowerBound(sorted, target)).as("trial %d: lowerBound(%s, %d)", trial, array, target)
                        .isEqualTo(firstAtLeast);
                assertThat(Search.upperBound(sorted, target)).as("trial %d: upperBound(%s, %d)", trial, array, target)
                        .isEqualTo(firstAbove);
                assertThat(Search.countInRange(sorted, low, high))
                        .as("trial %d: countInRange(%s, %d, %d)", trial, array, low, high)
                        .isEqualTo(inRange);
            }
        }
    }
}
