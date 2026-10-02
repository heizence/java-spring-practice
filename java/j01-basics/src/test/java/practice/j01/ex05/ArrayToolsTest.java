package practice.j01.ex05;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J01-05 · Arrays and methods")
class ArrayToolsTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("max() returns the largest value")
        void max() {
            assertThat(ArrayTools.max(new int[] {3, 9, 4})).isEqualTo(9);
        }

        @Test
        @DisplayName("reverseInPlace() reverses the array itself")
        void reverse() {
            int[] values = {1, 2, 3, 4};

            ArrayTools.reverseInPlace(values);

            assertThat(values).containsExactly(4, 3, 2, 1);
        }

        @Test
        @DisplayName("rotateRight() moves the elements to the right, wrapping around")
        void rotate() {
            assertThat(ArrayTools.rotateRight(new int[] {1, 2, 3, 4, 5}, 2)).containsExactly(4, 5, 1, 2, 3);
        }

        @Test
        @DisplayName("average() keeps the fraction: average(1, 2) is 1.5")
        void average() {
            assertThat(ArrayTools.average(1, 2)).isEqualTo(1.5);
        }

        @Test
        @DisplayName("indexOf() finds an int, or returns -1")
        void indexOfInt() {
            int[] values = {5, 7, 9};

            assertThat(ArrayTools.indexOf(values, 7)).isEqualTo(1);
            assertThat(ArrayTools.indexOf(values, 8)).isEqualTo(-1);
        }

        @Test
        @DisplayName("indexOf() finds a String, or returns -1")
        void indexOfString() {
            String[] values = {"Kim", "Lee", "Park"};

            assertThat(ArrayTools.indexOf(values, "Lee")).isEqualTo(1);
            assertThat(ArrayTools.indexOf(values, "Choi")).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("max() works when every value is negative")
        void maxAllNegative() {
            assertThat(ArrayTools.max(new int[] {-8, -3, -5})).isEqualTo(-3);
        }

        @Test
        @DisplayName("max() of a single element is that element")
        void maxSingle() {
            assertThat(ArrayTools.max(new int[] {42})).isEqualTo(42);
        }

        @Test
        @DisplayName("max() handles Integer.MIN_VALUE and Integer.MAX_VALUE")
        void maxExtremes() {
            int[] extremes = {0, Integer.MAX_VALUE, Integer.MIN_VALUE};

            assertThat(ArrayTools.max(new int[] {Integer.MIN_VALUE})).isEqualTo(Integer.MIN_VALUE);
            assertThat(ArrayTools.max(extremes)).isEqualTo(Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("max() doesn't change the array you pass in")
        void maxKeepsInput() {
            int[] values = {3, 1, 2};

            ArrayTools.max(values);

            assertThat(values).containsExactly(3, 1, 2);
        }

        @Test
        @DisplayName("reverseInPlace() reverses an odd number of elements")
        void reverseOddLength() {
            int[] values = {1, 2, 3, 4, 5};

            ArrayTools.reverseInPlace(values);

            assertThat(values).containsExactly(5, 4, 3, 2, 1);
        }

        @Test
        @DisplayName("reverseInPlace() handles an empty array and a single element")
        void reverseTiny() {
            int[] empty = {};
            int[] single = {7};

            ArrayTools.reverseInPlace(empty);
            ArrayTools.reverseInPlace(single);

            assertThat(empty).isEmpty();
            assertThat(single).containsExactly(7);
        }

        @ParameterizedTest(name = "k = {0}")
        @ValueSource(ints = {0, 5, -5, 10})
        @DisplayName("rotateRight() by 0 or a multiple of the length keeps the order")
        void rotateFullTurns(int k) {
            assertThat(ArrayTools.rotateRight(new int[] {1, 2, 3, 4, 5}, k)).containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("rotateRight() by more than the length wraps around: 7 is the same as 2")
        void rotateMoreThanLength() {
            assertThat(ArrayTools.rotateRight(new int[] {1, 2, 3, 4, 5}, 7)).containsExactly(4, 5, 1, 2, 3);
        }

        @ParameterizedTest(name = "k = {0}")
        @ValueSource(ints = {-1, -6})
        @DisplayName("rotateRight() with a negative k moves the elements to the left")
        void rotateLeft(int k) {
            assertThat(ArrayTools.rotateRight(new int[] {1, 2, 3, 4, 5}, k)).containsExactly(2, 3, 4, 5, 1);
        }

        @ParameterizedTest(name = "k = {0}")
        @ValueSource(ints = {Integer.MIN_VALUE, Integer.MAX_VALUE})
        @DisplayName("rotateRight() handles the smallest and largest int as k (both are the same as 2 here)")
        void rotateExtremeK(int k) {
            assertThat(ArrayTools.rotateRight(new int[] {1, 2, 3, 4, 5}, k)).containsExactly(4, 5, 1, 2, 3);
        }

        @ParameterizedTest(name = "k = {0}")
        @ValueSource(ints = {0, 3, -3})
        @DisplayName("rotateRight() of an empty array is an empty array")
        void rotateEmpty(int k) {
            assertThat(ArrayTools.rotateRight(new int[0], k)).isEmpty();
        }

        @Test
        @DisplayName("rotateRight() returns a new array and doesn't change the one you pass in")
        void rotateKeepsInput() {
            int[] values = {1, 2, 3};

            int[] rotated = ArrayTools.rotateRight(values, 1);

            assertThat(values).containsExactly(1, 2, 3);
            assertThat(rotated).isNotSameAs(values).containsExactly(3, 1, 2);
        }

        @Test
        @DisplayName("average() of a single value is that value")
        void averageSingle() {
            assertThat(ArrayTools.average(7)).isEqualTo(7.0);
        }

        @Test
        @DisplayName("average() works with negative values")
        void averageNegative() {
            assertThat(ArrayTools.average(-1, -2)).isEqualTo(-1.5);
        }

        @Test
        @DisplayName("average() doesn't overflow for very large and very small values")
        void averageNoOverflow() {
            assertThat(ArrayTools.average(Integer.MAX_VALUE, Integer.MAX_VALUE)).isEqualTo(2147483647.0);
            assertThat(ArrayTools.average(Integer.MIN_VALUE, Integer.MIN_VALUE)).isEqualTo(-2147483648.0);
        }

        @Test
        @DisplayName("average() also accepts an array")
        void averageOfArray() {
            int[] values = {1, 2, 3, 4};

            assertThat(ArrayTools.average(values)).isEqualTo(2.5);
        }

        @Test
        @DisplayName("indexOf() returns the first index when a value appears twice")
        void indexOfFirst() {
            assertThat(ArrayTools.indexOf(new int[] {4, 2, 4}, 4)).isZero();
            assertThat(ArrayTools.indexOf(new String[] {"b", "a", "b"}, "b")).isZero();
        }

        @Test
        @DisplayName("indexOf() returns -1 for an empty array")
        void indexOfEmpty() {
            assertThat(ArrayTools.indexOf(new int[0], 1)).isEqualTo(-1);
            assertThat(ArrayTools.indexOf(new String[0], "a")).isEqualTo(-1);
        }

        @Test
        @DisplayName("indexOf() compares Strings with equals(), so a different object with the same text counts")
        void indexOfUsesEquals() {
            String[] values = {"Kim", "Lee"};
            String lee = new String("Lee"); // same text, different object

            assertThat(ArrayTools.indexOf(values, lee)).isEqualTo(1);
        }

        @Test
        @DisplayName("indexOf() skips null elements and can search for null")
        void indexOfWithNulls() {
            String[] values = {"Kim", null, "Lee", null};

            assertThat(ArrayTools.indexOf(values, "Lee")).isEqualTo(2);
            assertThat(ArrayTools.indexOf(values, null)).isEqualTo(1);
            assertThat(ArrayTools.indexOf(new String[] {"Kim"}, null)).isEqualTo(-1);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("rotateRight() matches Collections.rotate for 1,000 random arrays and steps")
        void rotateMatchesJdk() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            for (int step = 0; step < 1_000; step++) {
                int[] values = random.ints(random.nextInt(8), -50, 50).toArray();
                int k = random.nextInt(-20, 21);
                List<Integer> expected = new ArrayList<>(Arrays.stream(values).boxed().toList());
                Collections.rotate(expected, k); // the JDK's rotate moves elements to the right, like ours

                int[] rotated = ArrayTools.rotateRight(values, k);

                assertThat(rotated)
                        .as("rotateRight(%s, %d) at step %d", Arrays.toString(values), k, step)
                        .containsExactly(expected.stream().mapToInt(Integer::intValue).toArray());
            }
        }

        @Test
        @DisplayName("reverseInPlace() matches Collections.reverse for 1,000 random arrays")
        void reverseMatchesJdk() {
            var random = new Random(42);
            for (int step = 0; step < 1_000; step++) {
                int[] values = random.ints(random.nextInt(8), -50, 50).toArray();
                List<Integer> expected = new ArrayList<>(Arrays.stream(values).boxed().toList());
                Collections.reverse(expected);
                String before = Arrays.toString(values);

                ArrayTools.reverseInPlace(values);

                assertThat(values)
                        .as("reverseInPlace(%s) at step %d", before, step)
                        .containsExactly(expected.stream().mapToInt(Integer::intValue).toArray());
            }
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("max() rejects an empty array")
        void maxEmpty() {
            assertThatThrownBy(() -> ArrayTools.max(new int[0])).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("average() rejects a call without values")
        void averageEmpty() {
            assertThatThrownBy(() -> ArrayTools.average()).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("every method rejects a null array")
        void rejectsNull() {
            assertThatThrownBy(() -> ArrayTools.max(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ArrayTools.reverseInPlace(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ArrayTools.rotateRight(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ArrayTools.average((int[]) null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ArrayTools.indexOf((int[]) null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ArrayTools.indexOf((String[]) null, "Kim"))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
