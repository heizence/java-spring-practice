package practice.j06.ex05;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.Gatherer;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J06-05 · Stream gatherers")
class GatheringTest {

    /** An event from some source; the source may be unknown ({@code null}). */
    private record Event(String id, String source) {
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("batches() splits items into batches of the given size; the last one may be smaller")
        void batches() {
            assertThat(Gathering.batches(List.of("a", "b", "c", "d", "e"), 2))
                    .containsExactly(List.of("a", "b"), List.of("c", "d"), List.of("e"));
        }

        @Test
        @DisplayName("movingAverages() averages each window of consecutive values")
        void movingAverages() {
            assertThat(Gathering.movingAverages(List.of(1, 2, 3, 4), 2)).containsExactly(1.5, 2.5, 3.5);
        }

        @Test
        @DisplayName("runningBalances() gives the balance after each amount")
        void runningBalances() {
            assertThat(Gathering.runningBalances(100, List.of(50L, -30L, 20L))).containsExactly(150L, 120L, 140L);
        }

        @Test
        @DisplayName("digitsToNumber() turns digits into a number")
        void digitsToNumber() {
            assertThat(Gathering.digitsToNumber(List.of(1, 2, 3))).isEqualTo(123);
        }

        @Test
        @DisplayName("distinctBy() keeps the first element for each key")
        void distinctBy() {
            List<String> firstPerLetter = Stream.of("apple", "avocado", "banana", "blueberry", "cherry")
                    .gather(Gathering.distinctBy((String word) -> word.charAt(0)))
                    .toList();

            assertThat(firstPerLetter).containsExactly("apple", "banana", "cherry");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("batches() of no items is no batches")
        void batchesOfNothing() {
            assertThat(Gathering.batches(List.of(), 3)).isEmpty();
        }

        @Test
        @DisplayName("batches() puts everything in one batch when the size is larger than the list")
        void batchLargerThanList() {
            assertThat(Gathering.batches(List.of(1, 2), 5)).containsExactly(List.of(1, 2));
        }

        @Test
        @DisplayName("batches() with a size that divides the list exactly gives full batches only")
        void batchesDivideExactly() {
            assertThat(Gathering.batches(List.of(1, 2, 3, 4), 2)).containsExactly(List.of(1, 2), List.of(3, 4));
        }

        @Test
        @DisplayName("batches are unmodifiable")
        void batchesAreUnmodifiable() {
            List<List<Integer>> batches = Gathering.batches(List.of(1, 2, 3), 2);

            assertThatThrownBy(() -> batches.getFirst().add(9)).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("movingAverages() with exactly `window` values gives one average")
        void movingAverageOneWindow() {
            assertThat(Gathering.movingAverages(List.of(2, 4, 9), 3)).containsExactly(5.0);
        }

        @Test
        @DisplayName("movingAverages() with fewer values than the window is empty: there's no full window")
        void movingAverageTooFewValues() {
            assertThat(Gathering.movingAverages(List.of(1, 2), 3)).isEmpty();
        }

        @Test
        @DisplayName("movingAverages() with a window of 1 returns each value")
        void movingAverageWindowOne() {
            assertThat(Gathering.movingAverages(List.of(3, -1, 4), 1)).containsExactly(3.0, -1.0, 4.0);
        }

        @Test
        @DisplayName("movingAverages() of no values is empty")
        void movingAverageEmpty() {
            assertThat(Gathering.movingAverages(List.of(), 2)).isEmpty();
        }

        @Test
        @DisplayName("runningBalances() of no amounts is empty")
        void runningBalancesEmpty() {
            assertThat(Gathering.runningBalances(100, List.of())).isEmpty();
        }

        @Test
        @DisplayName("runningBalances() can go below zero")
        void runningBalancesNegative() {
            assertThat(Gathering.runningBalances(10, List.of(-25L, 5L))).containsExactly(-15L, -10L);
        }

        @Test
        @DisplayName("digitsToNumber() of no digits is 0")
        void noDigits() {
            assertThat(Gathering.digitsToNumber(List.of())).isZero();
        }

        @Test
        @DisplayName("digitsToNumber() ignores leading zeros")
        void leadingZeros() {
            assertThat(Gathering.digitsToNumber(List.of(0, 0, 7))).isEqualTo(7);
        }

        @Test
        @DisplayName("digitsToNumber() handles the largest long")
        void largestLong() {
            List<Integer> digits = "9223372036854775807".chars().map(c -> c - '0').boxed().toList();

            assertThat(Gathering.digitsToNumber(digits)).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("distinctBy() keeps the encounter order of the kept elements")
        void distinctByKeepsOrder() {
            // Keys (n % 5): 0, 3, 0, 4, 3, 1. The second 0 and the second 3 are dropped.
            List<Integer> result = Stream.of(5, 3, 15, 9, 13, 1)
                    .gather(Gathering.distinctBy((Integer n) -> n % 5))
                    .toList();

            assertThat(result).containsExactly(5, 3, 9, 1);
        }

        @Test
        @DisplayName("distinctBy() treats null as a key like any other")
        void distinctByNullKey() {
            var e1 = new Event("e1", null);
            var e2 = new Event("e2", "web");
            var e3 = new Event("e3", null);
            var e4 = new Event("e4", "web");

            List<Event> result = Stream.of(e1, e2, e3, e4).gather(Gathering.distinctBy(Event::source)).toList();

            assertThat(result).containsExactly(e1, e2);
        }

        @Test
        @DisplayName("the same distinctBy() gatherer can be used for two streams: each starts with nothing seen")
        void distinctByIsReusable() {
            Gatherer<String, ?, String> byLength = Gathering.distinctBy(String::length);

            List<String> first = Stream.of("a", "b", "cc").gather(byLength).toList();
            List<String> second = Stream.of("d", "ee").gather(byLength).toList();

            assertThat(first).containsExactly("a", "cc");
            assertThat(second).containsExactly("d", "ee");
        }

        @Test
        @DisplayName("distinctBy() works with limit() on an endless stream")
        void distinctByWithLimit() {
            List<Integer> result = Stream.iterate(1, n -> n + 1)
                    .gather(Gathering.distinctBy((Integer n) -> n % 4))
                    .limit(3)
                    .toList();

            assertThat(result).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("distinctBy() of an empty stream is empty")
        void distinctByEmpty() {
            assertThat(Stream.<String>empty().gather(Gathering.distinctBy(String::length)).toList()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "size {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("batches() rejects a size below 1")
        void batchesRejectInvalidSize(int size) {
            assertThatThrownBy(() -> Gathering.batches(List.of(1), size)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "window {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("movingAverages() rejects a window below 1")
        void movingAveragesRejectInvalidWindow(int window) {
            assertThatThrownBy(() -> Gathering.movingAverages(List.of(1, 2), window))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "digit {0}")
        @ValueSource(ints = {-1, 10})
        @DisplayName("digitsToNumber() rejects a digit outside 0 to 9")
        void digitsRejectInvalidDigit(int digit) {
            assertThatThrownBy(() -> Gathering.digitsToNumber(List.of(1, digit, 2)))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("digitsToNumber() throws ArithmeticException when the number doesn't fit in a long")
        void digitsOverflow() {
            List<Integer> digits = "9223372036854775808".chars().map(c -> c - '0').boxed().toList();

            assertThatThrownBy(() -> Gathering.digitsToNumber(digits)).isInstanceOf(ArithmeticException.class);
        }

        @Test
        @DisplayName("distinctBy() rejects a null key extractor right away")
        void distinctByRejectsNull() {
            assertThatThrownBy(() -> Gathering.distinctBy(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("every method rejects a null list")
        void rejectsNullLists() {
            assertThatThrownBy(() -> Gathering.batches(null, 2)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Gathering.movingAverages(null, 2)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Gathering.runningBalances(0, null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Gathering.digitsToNumber(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
