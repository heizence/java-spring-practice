package practice.j04.ex02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.j04.support.GenericSignature;

/**
 * Tests for J04-02.
 *
 * <p>The behavior tests call the methods only in ways that compile with the strict signatures of the exercise
 * <em>and</em> with the flexible ones you write. Calls that need the flexible signatures would stop this file
 * from compiling while you work, so the "Signatures" tests check those through reflection instead.
 */
@DisplayName("J04-02 · Bounded types and wildcards")
class CollectionToolsTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("max() returns the largest element in natural order")
        void maxNaturalOrder() {
            assertThat(CollectionTools.max(List.of(3, 7, 5), Comparator.naturalOrder())).isEqualTo(7);
        }

        @Test
        @DisplayName("max() uses the comparator: the longest title")
        void maxWithComparator() {
            List<String> titles = List.of("Refactoring", "Designing Data-Intensive Applications", "Clean Code");

            assertThat(CollectionTools.max(titles, Comparator.comparingInt(String::length)))
                    .isEqualTo("Designing Data-Intensive Applications");
        }

        @Test
        @DisplayName("copyIf() copies the matching elements in order and returns how many it copied")
        void copyIf() {
            List<Integer> target = new ArrayList<>();

            int copied = CollectionTools.copyIf(List.of(3, -1, 4, -1, 5), n -> n > 0, target);

            assertThat(copied).isEqualTo(3);
            assertThat(target).containsExactly(3, 4, 5);
        }

        @Test
        @DisplayName("sum() adds the numbers")
        void sum() {
            assertThat(CollectionTools.sum(List.of(1, 2, 3))).isEqualTo(6.0);
        }

        @Test
        @DisplayName("clamp() returns the value when it is inside the range")
        void clampInside() {
            assertThat(CollectionTools.clamp(5, 1, 10)).isEqualTo(5);
        }

        @Test
        @DisplayName("clamp() returns min for a smaller value and max for a larger one")
        void clampOutside() {
            assertThat(CollectionTools.clamp(-3, 1, 10)).isEqualTo(1);
            assertThat(CollectionTools.clamp(42, 1, 10)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("max() returns the first of several equally large elements")
        void maxTies() {
            List<String> fruits = List.of("pear", "fig", "plum", "kiwi");

            assertThat(CollectionTools.max(fruits, Comparator.comparingInt(String::length))).isEqualTo("pear");
        }

        @Test
        @DisplayName("max() of a single element is that element")
        void maxSingle() {
            assertThat(CollectionTools.max(List.of(42), Comparator.naturalOrder())).isEqualTo(42);
        }

        @Test
        @DisplayName("max() with a reversed comparator returns the smallest element")
        void maxReversed() {
            assertThat(CollectionTools.max(List.of(3, 7, 5), Comparator.reverseOrder())).isEqualTo(3);
        }

        @Test
        @DisplayName("copyIf() keeps what target already holds and counts only the elements it added")
        void copyIfKeepsTarget() {
            List<Integer> target = new ArrayList<>(List.of(1, 2));

            int copied = CollectionTools.copyIf(List.of(3, -1, 4), n -> n > 0, target);

            assertThat(copied).isEqualTo(2);
            assertThat(target).containsExactly(1, 2, 3, 4);
        }

        @Test
        @DisplayName("copyIf() returns 0 and leaves target alone when nothing matches")
        void copyIfNoMatch() {
            List<Integer> target = new ArrayList<>(List.of(9));

            int copied = CollectionTools.copyIf(List.of(-1, -2), n -> n > 0, target);

            assertThat(copied).isZero();
            assertThat(target).containsExactly(9);
        }

        @Test
        @DisplayName("copyIf() copies nothing from an empty source")
        void copyIfEmptySource() {
            List<String> target = new ArrayList<>();

            assertThat(CollectionTools.copyIf(List.of(), s -> true, target)).isZero();
            assertThat(target).isEmpty();
        }

        @Test
        @DisplayName("sum() adds different kinds of numbers: Integer, Long, Double and BigDecimal")
        void sumMixed() {
            assertThat(CollectionTools.sum(List.of(1, 2L, 0.5, new BigDecimal("1.25")))).isEqualTo(4.75);
        }

        @Test
        @DisplayName("sum() of an empty collection is 0.0")
        void sumEmpty() {
            assertThat(CollectionTools.sum(List.of())).isZero();
        }

        @Test
        @DisplayName("sum() handles negative numbers and fractions")
        void sumNegative() {
            assertThat(CollectionTools.sum(List.of(-1.5, 2, -3))).isEqualTo(-2.5);
        }

        @Test
        @DisplayName("clamp() returns the value itself at both ends of the range")
        void clampBoundaries() {
            assertThat(CollectionTools.clamp(1, 1, 10)).isEqualTo(1);
            assertThat(CollectionTools.clamp(10, 1, 10)).isEqualTo(10);
        }

        @Test
        @DisplayName("clamp() works for a range of one value (min equals max)")
        void clampSingleValueRange() {
            assertThat(CollectionTools.clamp(5, 7, 7)).isEqualTo(7);
        }

        @Test
        @DisplayName("clamp() works with Strings, in alphabetical order")
        void clampStrings() {
            assertThat(CollectionTools.clamp("kiwi", "banana", "cherry")).isEqualTo("cherry");
            assertThat(CollectionTools.clamp("apple", "banana", "cherry")).isEqualTo("banana");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("max() throws NoSuchElementException for an empty collection")
        void maxEmpty() {
            assertThatThrownBy(() -> CollectionTools.max(List.<Integer>of(), Comparator.naturalOrder()))
                    .isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("max() rejects null arguments")
        void maxNull() {
            assertThatThrownBy(() -> CollectionTools.max(null, Comparator.<Integer>naturalOrder()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CollectionTools.max(List.of(1), null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("copyIf() rejects null arguments")
        void copyIfNull() {
            List<Integer> target = new ArrayList<>();

            assertThatThrownBy(() -> CollectionTools.copyIf(null, n -> true, target))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CollectionTools.copyIf(List.of(1), null, target))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CollectionTools.copyIf(List.of(1), n -> true, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("sum() rejects a null collection and a null element")
        void sumNull() {
            assertThatThrownBy(() -> CollectionTools.sum(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CollectionTools.sum(Arrays.asList(1, null)))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("clamp() rejects min greater than max, naming both")
        void clampInvalidRange() {
            assertThatThrownBy(() -> CollectionTools.clamp(5, 9, 2))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("9")
                    .hasMessageContaining("2");
        }

        @Test
        @DisplayName("clamp() rejects null arguments")
        void clampNull() {
            assertThatThrownBy(() -> CollectionTools.clamp(null, 1, 10)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CollectionTools.clamp(5, null, 10)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> CollectionTools.clamp(5, 1, null)).isInstanceOf(NullPointerException.class);
        }
    }

    /**
     * Checks the signatures through reflection. Each failure message says which parameter is still too strict and
     * why, followed by your current signature.
     */
    @Nested
    @DisplayName("Signatures")
    class Signatures {

        @Test
        @DisplayName("max(): items is a producer, so it accepts a collection of any subtype of T")
        void maxItems() {
            var signature = GenericSignature.of(CollectionTools.class, "max");

            assertThat(signature.parameter(0))
                    .withFailMessage("max() only reads from items, so items should also accept collections of "
                            + "subtypes of T. Your signature: %s", signature)
                    .isEqualTo("Collection<? extends T>");
        }

        @Test
        @DisplayName("max(): comparator is a consumer, so it accepts a comparator for any supertype of T")
        void maxComparator() {
            var signature = GenericSignature.of(CollectionTools.class, "max");

            assertThat(signature.parameter(1))
                    .withFailMessage("max() passes T values to comparator, so it should also accept comparators "
                            + "of supertypes of T. Your signature: %s", signature)
                    .isEqualTo("Comparator<? super T>");
        }

        @Test
        @DisplayName("copyIf(): source is a producer, so it accepts a list of any subtype of T")
        void copyIfSource() {
            var signature = GenericSignature.of(CollectionTools.class, "copyIf");

            assertThat(signature.parameter(0))
                    .withFailMessage("copyIf() only reads from source, so source should also accept lists of "
                            + "subtypes of T. Your signature: %s", signature)
                    .isEqualTo("List<? extends T>");
        }

        @Test
        @DisplayName("copyIf(): condition is a consumer, so it accepts a predicate for any supertype of T")
        void copyIfCondition() {
            var signature = GenericSignature.of(CollectionTools.class, "copyIf");

            assertThat(signature.parameter(1))
                    .withFailMessage("condition receives T values, so it should also accept predicates of "
                            + "supertypes of T. Your signature: %s", signature)
                    .isEqualTo("Predicate<? super T>");
        }

        @Test
        @DisplayName("copyIf(): target is a consumer, so it accepts a list of any supertype of T")
        void copyIfTarget() {
            var signature = GenericSignature.of(CollectionTools.class, "copyIf");

            assertThat(signature.parameter(2))
                    .withFailMessage("copyIf() only adds T values to target, so target should also accept lists of "
                            + "supertypes of T. Your signature: %s", signature)
                    .isEqualTo("List<? super T>");
        }

        @Test
        @DisplayName("sum(): accepts a collection of any Number type, such as a List<Integer>")
        void sumNumbers() {
            var signature = GenericSignature.of(CollectionTools.class, "sum");

            assertThat(signature.toString())
                    .withFailMessage("sum() should also accept a List<Integer>, which isn't a Collection<Number>: "
                            + "generic types are invariant. Your signature: %s", signature)
                    .isIn("double sum(Collection<? extends Number>)",
                            "<T extends Number> double sum(Collection<T>)",
                            "<T extends Number> double sum(Collection<? extends T>)");
        }

        @Test
        @DisplayName("clamp(): T may be comparable through a supertype, like LocalDate")
        void clampBound() {
            var signature = GenericSignature.of(CollectionTools.class, "clamp");

            assertThat(signature.typeParameters())
                    .withFailMessage("The bound of T should also allow LocalDate, which is a "
                            + "Comparable<ChronoLocalDate>. Your signature: %s", signature)
                    .isEqualTo("<T extends Comparable<? super T>>");
        }
    }
}
