package practice.j02.ex04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J02-04 · Composition over inheritance")
class InstrumentedSetTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("add() counts one attempt per call")
        void addCountsOnce() {
            var set = new InstrumentedSet<String>(new HashSet<>());

            set.add("apple");
            set.add("banana");

            assertThat(set.addCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("addAll() counts each element once")
        void addAllCountsEachElementOnce() {
            var set = new InstrumentedSet<String>(new HashSet<>());

            set.addAll(List.of("apple", "banana", "cherry"));

            assertThat(set.addCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("add() and addAll() together count every element once")
        void addAndAddAll() {
            var set = new InstrumentedSet<String>(new HashSet<>());

            set.add("apple");
            set.addAll(List.of("banana", "cherry"));
            set.add("durian");

            assertThat(set.addCount()).isEqualTo(4);
        }

        @Test
        @DisplayName("a new set starts at 0 attempts, even when the wrapped set already has elements")
        void startsAtZero() {
            var set = new InstrumentedSet<>(new HashSet<>(Set.of("apple", "banana", "cherry")));

            assertThat(set.addCount()).isZero();
        }

        @Test
        @DisplayName("works like a set: contains, size and remove")
        void worksLikeASet() {
            var set = new InstrumentedSet<String>(new HashSet<>());
            set.addAll(List.of("apple", "banana"));

            boolean removed = set.remove("apple");

            assertThat(removed).isTrue();
            assertThat(set.contains("apple")).isFalse();
            assertThat(set.contains("banana")).isTrue();
            assertThat(set.size()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("counts duplicates as attempts but stores each element once")
        void duplicates() {
            var set = new InstrumentedSet<String>(new HashSet<>());

            set.add("apple");
            set.add("apple");
            set.addAll(List.of("apple", "banana", "banana"));

            assertThat(set.addCount()).isEqualTo(5);
            assertThat(set).containsExactlyInAnyOrder("apple", "banana");
        }

        @Test
        @DisplayName("addAll() of an empty collection counts nothing")
        void addAllEmpty() {
            var set = new InstrumentedSet<String>(new HashSet<>());

            set.addAll(List.of());

            assertThat(set.addCount()).isZero();
        }

        @Test
        @DisplayName("elements added through it end up in the wrapped set")
        void writesThrough() {
            var wrapped = new HashSet<String>();
            var set = new InstrumentedSet<>(wrapped);

            set.add("apple");
            set.addAll(List.of("banana", "cherry"));

            assertThat(wrapped).containsExactlyInAnyOrder("apple", "banana", "cherry");
        }

        @Test
        @DisplayName("changes made directly to the wrapped set show through")
        void readsThrough() {
            var wrapped = new HashSet<>(Set.of("apple"));
            var set = new InstrumentedSet<>(wrapped);

            wrapped.add("banana");
            wrapped.remove("apple");

            assertThat(set).containsExactly("banana");
        }

        @Test
        @DisplayName("keeps the sort order of a wrapped TreeSet")
        void keepsTreeSetOrder() {
            var set = new InstrumentedSet<Integer>(new TreeSet<>(Comparator.reverseOrder()));

            set.addAll(List.of(3, 1, 5, 2, 4));

            assertThat(set).containsExactly(5, 4, 3, 2, 1);
        }

        @Test
        @DisplayName("keeps the insertion order of a wrapped LinkedHashSet")
        void keepsLinkedHashSetOrder() {
            var set = new InstrumentedSet<String>(new LinkedHashSet<>());

            set.addAll(List.of("cherry", "apple", "banana"));

            assertThat(set).containsExactly("cherry", "apple", "banana");
        }

        @Test
        @DisplayName("uses the wrapped set's comparator to find duplicates")
        void usesComparatorForDuplicates() {
            var set = new InstrumentedSet<String>(new TreeSet<>(String.CASE_INSENSITIVE_ORDER));

            set.add("Apple");
            set.add("APPLE");
            set.add("apple");

            assertThat(set).containsExactly("Apple");
            assertThat(set.addCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("removing elements doesn't change the count")
        void removalsDontCount() {
            var set = new InstrumentedSet<String>(new HashSet<>());
            set.addAll(List.of("apple", "banana", "cherry"));

            set.remove("apple");
            set.removeAll(List.of("banana"));
            set.clear();

            assertThat(set.addCount()).isEqualTo(3);
            assertThat(set).isEmpty();
        }

        @Test
        @DisplayName("is equal to any set with the same elements, both ways, with the same hash code")
        void equalsAndHashCode() {
            var set = new InstrumentedSet<String>(new TreeSet<>());
            set.addAll(List.of("apple", "banana"));
            Set<String> plain = Set.of("banana", "apple");

            assertThat(set).isEqualTo(plain);
            assertThat(plain).isEqualTo(set);
            assertThat(set.hashCode()).isEqualTo(plain.hashCode());
        }

        @Test
        @DisplayName("toString() shows the elements the way the wrapped set does")
        void toStringLikeWrappedSet() {
            var set = new InstrumentedSet<String>(new TreeSet<>());

            set.addAll(List.of("cherry", "apple", "banana"));

            assertThat(set).hasToString("[apple, banana, cherry]");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a null set")
        void nullSet() {
            assertThatThrownBy(() -> new InstrumentedSet<String>(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("an unmodifiable wrapped set stays unmodifiable")
        void unmodifiableStaysUnmodifiable() {
            var set = new InstrumentedSet<>(Collections.unmodifiableSet(new HashSet<>(Set.of("apple"))));

            assertThatThrownBy(() -> set.add("banana")).isInstanceOf(UnsupportedOperationException.class);
            assertThat(set).containsExactly("apple");
        }

        @Test
        @DisplayName("a wrapped TreeSet still rejects null elements")
        void treeSetRejectsNull() {
            var set = new InstrumentedSet<String>(new TreeSet<>());

            assertThatThrownBy(() -> set.add(null)).isInstanceOf(NullPointerException.class);
            assertThat(set).isEmpty();
        }
    }
}
