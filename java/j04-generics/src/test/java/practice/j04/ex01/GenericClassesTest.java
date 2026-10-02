package practice.j04.ex01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J04-01 · Generic classes and methods")
class GenericClassesTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a Pair holds two values of different types")
        void pairHoldsValues() {
            Pair<String, Integer> book = new Pair<>("Clean Code", 3000);

            assertThat(book.first()).isEqualTo("Clean Code");
            assertThat(book.second()).isEqualTo(3000);
        }

        @Test
        @DisplayName("swap() returns a new pair with the values exchanged")
        void swap() {
            Pair<String, Integer> original = new Pair<>("one", 1);

            Pair<Integer, String> swapped = original.swap();

            assertThat(swapped).isEqualTo(new Pair<>(1, "one"));
            assertThat(original).isEqualTo(new Pair<>("one", 1));
        }

        @Test
        @DisplayName("mapFirst() converts the first value and keeps the second")
        void mapFirst() {
            Pair<String, Integer> book = new Pair<>("clean code", 3000);

            assertThat(book.mapFirst(String::toUpperCase)).isEqualTo(new Pair<>("CLEAN CODE", 3000));
        }

        @Test
        @DisplayName("mapSecond() converts the second value and keeps the first")
        void mapSecond() {
            Pair<String, Integer> book = new Pair<>("Clean Code", 3000);

            Pair<String, Double> withPrice = book.mapSecond(cents -> cents / 100.0);

            assertThat(withPrice).isEqualTo(new Pair<>("Clean Code", 30.0));
        }

        @Test
        @DisplayName("a Page holds its items, number, size and total")
        void pageHoldsValues() {
            var page = new Page<>(List.of("a", "b"), 1, 2, 5);

            assertThat(page.items()).containsExactly("a", "b");
            assertThat(page.number()).isEqualTo(1);
            assertThat(page.size()).isEqualTo(2);
            assertThat(page.totalElements()).isEqualTo(5);
        }

        @Test
        @DisplayName("totalPages() divides the total by the page size")
        void totalPages() {
            var page = new Page<>(List.of("a"), 0, 10, 20);

            assertThat(page.totalPages()).isEqualTo(2);
        }

        @Test
        @DisplayName("hasNext() is true on the first of two pages and false on the second")
        void hasNext() {
            var first = new Page<>(List.of("a"), 0, 10, 20);
            var second = new Page<>(List.of("b"), 1, 10, 20);

            assertThat(first.hasNext()).isTrue();
            assertThat(second.hasNext()).isFalse();
        }

        @Test
        @DisplayName("map() converts every item and keeps the page information")
        void mapPage() {
            Page<String> titles = new Page<>(List.of("Clean Code", "Refactoring"), 3, 2, 9);

            Page<Integer> lengths = titles.map(String::length);

            assertThat(lengths).isEqualTo(new Page<>(List.of(10, 11), 3, 2, 9));
        }

        @Test
        @DisplayName("firstOrDefault() returns the first element")
        void firstOrDefault() {
            assertThat(ListHelpers.firstOrDefault(List.of("Kim", "Lee"), "nobody")).isEqualTo("Kim");
        }

        @Test
        @DisplayName("firstOrDefault() returns the fallback for an empty list")
        void firstOrDefaultEmpty() {
            assertThat(ListHelpers.firstOrDefault(List.of(), "nobody")).isEqualTo("nobody");
        }

        @Test
        @DisplayName("lastN() returns the last n elements in their original order")
        void lastN() {
            assertThat(ListHelpers.lastN(List.of(1, 2, 3, 4, 5), 2)).containsExactly(4, 5);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("two pairs with equal values are equal, like any record")
        void pairEquality() {
            assertThat(new Pair<>("a", 1)).isEqualTo(new Pair<>("a", 1)).hasSameHashCodeAs(new Pair<>("a", 1));
            assertThat(new Pair<>("a", 1)).isNotEqualTo(new Pair<>(1, "a"));
        }

        @Test
        @DisplayName("mapFirst() can change the type of the first value")
        void mapFirstChangesType() {
            Pair<String, Integer> book = new Pair<>("Java", 25);

            Pair<Integer, Integer> lengths = book.mapFirst(String::length);

            assertThat(lengths).isEqualTo(new Pair<>(4, 25));
        }

        @Test
        @DisplayName("mapFirst() accepts a function written for a supertype, such as Function<Object, String>")
        void mapFirstWithSupertypeFunction() {
            Function<Object, String> describe = value -> "<" + value + ">";
            Pair<Integer, String> pair = new Pair<>(42, "answer");

            assertThat(pair.mapFirst(describe)).isEqualTo(new Pair<>("<42>", "answer"));
        }

        @ParameterizedTest(name = "{0} items at {1} per page need {2} pages", quoteTextArguments = false)
        @CsvSource({"21, 10, 3", "1, 10, 1", "10, 10, 1", "11, 10, 2", "7, 1, 7"})
        @DisplayName("totalPages() rounds a partial last page up")
        void totalPagesRoundsUp(long total, int size, long pages) {
            var page = new Page<String>(List.of(), 0, size, total);

            assertThat(page.totalPages()).isEqualTo(pages);
        }

        @Test
        @DisplayName("totalPages() is 0 and hasNext() is false when there are no items at all")
        void noItems() {
            var page = new Page<String>(List.of(), 0, 10, 0);

            assertThat(page.totalPages()).isZero();
            assertThat(page.hasNext()).isFalse();
        }

        @Test
        @DisplayName("totalPages() doesn't overflow for a total close to Long.MAX_VALUE")
        void totalPagesHugeTotal() {
            var page = new Page<String>(List.of(), 0, 10, Long.MAX_VALUE);

            assertThat(page.totalPages()).isEqualTo(922_337_203_685_477_581L);
        }

        @Test
        @DisplayName("hasNext() is false on the last page of a partial result")
        void hasNextOnLastPartialPage() {
            var secondToLast = new Page<>(List.of("x"), 1, 10, 21);
            var last = new Page<>(List.of("y"), 2, 10, 21);

            assertThat(secondToLast.hasNext()).isTrue();
            assertThat(last.hasNext()).isFalse();
        }

        @Test
        @DisplayName("hasNext() is false for a page number past the end")
        void hasNextPastTheEnd() {
            var page = new Page<String>(List.of(), 7, 10, 21);

            assertThat(page.hasNext()).isFalse();
        }

        @Test
        @DisplayName("hasNext() doesn't overflow for page number Integer.MAX_VALUE")
        void hasNextWithLargestPageNumber() {
            var page = new Page<String>(List.of(), Integer.MAX_VALUE, 10, 50);

            assertThat(page.hasNext()).isFalse();
        }

        @Test
        @DisplayName("a Page keeps its own copy: changing the original list doesn't change the page")
        void pageCopiesItems() {
            List<String> titles = new ArrayList<>(List.of("Clean Code"));
            var page = new Page<>(titles, 0, 10, 1);

            titles.add("Refactoring");

            assertThat(page.items()).containsExactly("Clean Code");
        }

        @Test
        @DisplayName("the items of a Page can't be modified")
        void pageItemsUnmodifiable() {
            var page = new Page<>(new ArrayList<>(List.of("Clean Code")), 0, 10, 1);

            assertThatThrownBy(() -> page.items().add("Refactoring"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("a Page may hold exactly size items")
        void pageFull() {
            var page = new Page<>(List.of("a", "b"), 0, 2, 2);

            assertThat(page.items()).hasSize(2);
        }

        @Test
        @DisplayName("map() on an empty page gives an empty page with the same numbers")
        void mapEmptyPage() {
            Page<String> empty = new Page<>(List.of(), 4, 10, 40);

            assertThat(empty.map(String::length)).isEqualTo(new Page<Integer>(List.of(), 4, 10, 40));
        }

        @Test
        @DisplayName("firstOrDefault() works for a list with one element")
        void firstOrDefaultSingle() {
            assertThat(ListHelpers.firstOrDefault(List.of(7), 0)).isEqualTo(7);
        }

        @Test
        @DisplayName("lastN() returns the whole list when n is larger than the list")
        void lastNMoreThanSize() {
            assertThat(ListHelpers.lastN(List.of(1, 2, 3), 10)).containsExactly(1, 2, 3);
        }

        @Test
        @DisplayName("lastN() returns an empty list for n = 0 and for an empty list")
        void lastNZero() {
            assertThat(ListHelpers.lastN(List.of(1, 2, 3), 0)).isEmpty();
            assertThat(ListHelpers.lastN(List.of(), 3)).isEmpty();
        }

        @Test
        @DisplayName("lastN() returns a copy: changing the original list later doesn't change the result")
        void lastNIsCopy() {
            List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4));
            List<Integer> lastTwo = ListHelpers.lastN(numbers, 2);

            numbers.set(3, 40);
            numbers.add(5);

            assertThat(lastTwo).containsExactly(3, 4);
        }

        @Test
        @DisplayName("lastN() returns a list that can't be modified")
        void lastNUnmodifiable() {
            List<Integer> lastTwo = ListHelpers.lastN(new ArrayList<>(List.of(1, 2, 3)), 2);

            assertThatThrownBy(() -> lastTwo.add(4)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a Pair rejects null values")
        void pairRejectsNull() {
            assertThatThrownBy(() -> new Pair<>(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Pair<>("a", null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("mapFirst() and mapSecond() reject a mapper that returns null")
        void mapperReturnsNull() {
            var pair = new Pair<>("a", 1);

            assertThatThrownBy(() -> pair.mapFirst(value -> null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> pair.mapSecond(value -> null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "number {0}")
        @ValueSource(ints = {-1, Integer.MIN_VALUE})
        @DisplayName("a Page rejects a negative page number")
        void pageRejectsNegativeNumber(int number) {
            assertThatThrownBy(() -> new Page<String>(List.of(), number, 10, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "size {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("a Page rejects a size below 1")
        void pageRejectsInvalidSize(int size) {
            assertThatThrownBy(() -> new Page<String>(List.of(), 0, size, 0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("a Page rejects a negative total")
        void pageRejectsNegativeTotal() {
            assertThatThrownBy(() -> new Page<String>(List.of(), 0, 10, -1))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("a Page rejects more items than its size")
        void pageRejectsTooManyItems() {
            assertThatThrownBy(() -> new Page<>(List.of("a", "b", "c"), 0, 2, 3))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("a Page rejects a null list and null items")
        void pageRejectsNull() {
            assertThatThrownBy(() -> new Page<String>(null, 0, 10, 0)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Page<>(Arrays.asList("a", null), 0, 10, 2))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("firstOrDefault() rejects a null list, a null fallback and a null first element")
        void firstOrDefaultRejectsNull() {
            assertThatThrownBy(() -> ListHelpers.firstOrDefault(null, "x")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ListHelpers.firstOrDefault(List.of("a"), null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ListHelpers.firstOrDefault(Arrays.asList(null, "b"), "x"))
                    .isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "n = {0}")
        @ValueSource(ints = {-1, Integer.MIN_VALUE})
        @DisplayName("lastN() rejects a negative n")
        void lastNRejectsNegative(int n) {
            assertThatThrownBy(() -> ListHelpers.lastN(List.of(1, 2), n)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("lastN() rejects a null list and a null element in the result")
        void lastNRejectsNull() {
            assertThatThrownBy(() -> ListHelpers.lastN(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ListHelpers.lastN(Arrays.asList("a", null, "c"), 2))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
