package practice.j05.ex02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J05-02 · Dynamic array")
class DynamicArrayTest {

    /** An array with the default capacity that holds {@code elements}. */
    private static DynamicArray<String> arrayOf(String... elements) {
        var array = new DynamicArray<String>();
        for (String element : elements) {
            array.add(element);
        }
        return array;
    }

    /** The elements in index order. Reads them with get(), so it works before iterator() does. */
    private static <E> List<E> contentsOf(DynamicArray<E> array) {
        List<E> contents = new ArrayList<>();
        for (int i = 0; i < array.size(); i++) {
            contents.add(array.get(i));
        }
        return contents;
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a new array is empty")
        void newArrayIsEmpty() {
            var array = new DynamicArray<String>();

            assertThat(array.size()).isZero();
            assertThat(array.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("add() appends elements, and get() reads them by index")
        void addAppends() {
            var array = new DynamicArray<String>();

            array.add("a");
            array.add("b");
            array.add("c");

            assertThat(array.size()).isEqualTo(3);
            assertThat(array.isEmpty()).isFalse();
            assertThat(array.get(0)).isEqualTo("a");
            assertThat(array.get(2)).isEqualTo("c");
        }

        @Test
        @DisplayName("set() replaces an element and returns the one it replaced")
        void setReplaces() {
            var array = arrayOf("a", "b", "c");

            String previous = array.set(1, "x");

            assertThat(previous).isEqualTo("b");
            assertThat(contentsOf(array)).containsExactly("a", "x", "c");
        }

        @Test
        @DisplayName("add(index, element) inserts and moves the later elements one place right")
        void insertMovesRight() {
            var array = arrayOf("a", "b", "c");

            array.add(1, "x");

            assertThat(contentsOf(array)).containsExactly("a", "x", "b", "c");
        }

        @Test
        @DisplayName("remove(index) returns the element and moves the later elements one place left")
        void removeMovesLeft() {
            var array = arrayOf("a", "b", "c", "d");

            String removed = array.remove(1);

            assertThat(removed).isEqualTo("b");
            assertThat(contentsOf(array)).containsExactly("a", "c", "d");
        }

        @Test
        @DisplayName("indexOf() and contains() find an element")
        void findsElement() {
            var array = arrayOf("a", "b", "c");

            assertThat(array.indexOf("c")).isEqualTo(2);
            assertThat(array.contains("c")).isTrue();
        }

        @Test
        @DisplayName("indexOf() returns -1 and contains() returns false for a missing element")
        void missingElement() {
            var array = arrayOf("a", "b", "c");

            assertThat(array.indexOf("z")).isEqualTo(-1);
            assertThat(array.contains("z")).isFalse();
        }

        @Test
        @DisplayName("a for-each loop visits the elements in index order")
        void iteratesInOrder() {
            var array = arrayOf("a", "b", "c");
            List<String> visited = new ArrayList<>();

            for (String element : array) {
                visited.add(element);
            }

            assertThat(visited).containsExactly("a", "b", "c");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("starts with a capacity of 10 by default")
        void defaultCapacity() {
            assertThat(new DynamicArray<String>().capacity()).isEqualTo(10);
        }

        @Test
        @DisplayName("starts with the capacity passed to the constructor")
        void customCapacity() {
            assertThat(new DynamicArray<String>(3).capacity()).isEqualTo(3);
        }

        @Test
        @DisplayName("doesn't grow while there is still room")
        void doesNotGrowEarly() {
            var array = new DynamicArray<String>(4);

            for (int i = 0; i < 4; i++) {
                array.add("x" + i);
            }

            assertThat(array.capacity()).isEqualTo(4);
        }

        @Test
        @DisplayName("doubles its capacity when an element is added to a full array, and keeps every element")
        void doublesWhenFull() {
            var array = new DynamicArray<String>(4);

            for (int i = 0; i < 5; i++) {
                array.add("x" + i);
            }

            assertThat(array.capacity()).isEqualTo(8);
            assertThat(contentsOf(array)).containsExactly("x0", "x1", "x2", "x3", "x4");
        }

        @Test
        @DisplayName("an array with capacity 1 grows to 2, 4 and 8")
        void capacityOneGrows() {
            var array = new DynamicArray<Integer>(1);
            List<Integer> capacities = new ArrayList<>();

            for (int i = 0; i < 5; i++) {
                array.add(i);
                capacities.add(array.capacity());
            }

            assertThat(capacities).containsExactly(1, 2, 4, 4, 8);
        }

        @Test
        @DisplayName("add(index, element) grows a full array, too")
        void insertIntoFullArray() {
            var array = new DynamicArray<String>(2);
            array.add("a");
            array.add("c");

            array.add(1, "b");

            assertThat(contentsOf(array)).containsExactly("a", "b", "c");
            assertThat(array.capacity()).isEqualTo(4);
        }

        @Test
        @DisplayName("add(size(), element) appends at the end")
        void insertAtEnd() {
            var array = arrayOf("a", "b");

            array.add(2, "c");

            assertThat(contentsOf(array)).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("add(0, element) inserts at the front, even into an empty array")
        void insertAtFront() {
            var array = new DynamicArray<String>();

            array.add(0, "b");
            array.add(0, "a");

            assertThat(contentsOf(array)).containsExactly("a", "b");
        }

        @Test
        @DisplayName("removes the first and the last element")
        void removeAtBothEnds() {
            var array = arrayOf("a", "b", "c");

            assertThat(array.remove(2)).isEqualTo("c");
            assertThat(array.remove(0)).isEqualTo("a");

            assertThat(contentsOf(array)).containsExactly("b");
        }

        @Test
        @DisplayName("removing the only element leaves an empty array that still works")
        void removeOnlyElement() {
            var array = arrayOf("a");

            array.remove(0);

            assertThat(array.isEmpty()).isTrue();
            array.add("b");
            assertThat(contentsOf(array)).containsExactly("b");
        }

        @Test
        @DisplayName("removing elements never shrinks the capacity")
        void capacityNeverShrinks() {
            var array = new DynamicArray<String>(2);
            array.add("a");
            array.add("b");
            array.add("c"); // capacity 4

            array.remove(0);
            array.remove(0);
            array.remove(0);

            assertThat(array.capacity()).isEqualTo(4);
        }

        @Test
        @DisplayName("indexOf() returns the index of the first of several equal elements")
        void indexOfFirstDuplicate() {
            var array = arrayOf("a", "b", "a", "b");

            assertThat(array.indexOf("b")).isEqualTo(1);
        }

        @Test
        @DisplayName("indexOf() and contains() compare with equals(), not ==")
        void comparesWithEquals() {
            var array = arrayOf("apple", "pear");
            String pear = new String("pear"); // the same text, but a different object

            assertThat(array.indexOf(pear)).isEqualTo(1);
            assertThat(array.contains(pear)).isTrue();
        }

        @Test
        @DisplayName("stores null elements, and indexOf(null) finds them")
        void nullElements() {
            var array = arrayOf("a", null, "c");

            assertThat(array.get(1)).isNull();
            assertThat(array.indexOf(null)).isEqualTo(1);
            assertThat(array.contains(null)).isTrue();
            assertThat(array.indexOf("c")).isEqualTo(2);
        }

        @Test
        @DisplayName("indexOf(null) returns -1 when there is no null element")
        void indexOfNullMissing() {
            var array = arrayOf("a", "b");

            assertThat(array.indexOf(null)).isEqualTo(-1);
            assertThat(array.contains(null)).isFalse();
        }

        @Test
        @DisplayName("the iterator of an empty array has no elements")
        void emptyIterator() {
            assertThat(new DynamicArray<String>().iterator().hasNext()).isFalse();
        }

        @Test
        @DisplayName("the iterator stops at size(), not at the end of the backing array")
        void iteratorStopsAtSize() {
            var array = new DynamicArray<String>(10);
            array.add("a");

            assertThat(array).containsExactly("a");
        }

        @Test
        @DisplayName("holds 100,000 elements")
        void largeInput() {
            var array = new DynamicArray<Integer>();

            for (int i = 0; i < 100_000; i++) {
                array.add(i);
            }

            assertThat(array.size()).isEqualTo(100_000);
            assertThat(array.get(99_999)).isEqualTo(99_999);
            assertThat(array.capacity()).isEqualTo(163_840); // 10 doubled 14 times
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "capacity {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("rejects a capacity below 1")
        void rejectsCapacityBelowOne(int capacity) {
            assertThatThrownBy(() -> new DynamicArray<String>(capacity))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "index {0} with size 3")
        @ValueSource(ints = {-1, 3, 4})
        @DisplayName("get() rejects an index outside 0 to size() - 1")
        void getRejectsBadIndex(int index) {
            var array = arrayOf("a", "b", "c");

            assertThatThrownBy(() -> array.get(index)).isInstanceOf(IndexOutOfBoundsException.class);
        }

        @ParameterizedTest(name = "index {0} with size 3")
        @ValueSource(ints = {-1, 3, 4})
        @DisplayName("set() rejects an index outside 0 to size() - 1 and changes nothing")
        void setRejectsBadIndex(int index) {
            var array = arrayOf("a", "b", "c");

            assertThatThrownBy(() -> array.set(index, "x")).isInstanceOf(IndexOutOfBoundsException.class);
            assertThat(contentsOf(array)).containsExactly("a", "b", "c");
        }

        @ParameterizedTest(name = "index {0} with size 3")
        @ValueSource(ints = {-1, 3, 4})
        @DisplayName("remove() rejects an index outside 0 to size() - 1 and changes nothing")
        void removeRejectsBadIndex(int index) {
            var array = arrayOf("a", "b", "c");

            assertThatThrownBy(() -> array.remove(index)).isInstanceOf(IndexOutOfBoundsException.class);
            assertThat(contentsOf(array)).containsExactly("a", "b", "c");
        }

        @ParameterizedTest(name = "index {0} with size 3")
        @ValueSource(ints = {-1, 4, 5})
        @DisplayName("add(index, element) rejects an index outside 0 to size() and changes nothing")
        void insertRejectsBadIndex(int index) {
            var array = arrayOf("a", "b", "c");

            assertThatThrownBy(() -> array.add(index, "x")).isInstanceOf(IndexOutOfBoundsException.class);
            assertThat(contentsOf(array)).containsExactly("a", "b", "c");
        }

        @Test
        @DisplayName("a rejected add(index, element) on a full array doesn't grow it")
        void failedInsertDoesNotGrow() {
            var array = new DynamicArray<String>(2);
            array.add("a");
            array.add("b");

            assertThatThrownBy(() -> array.add(5, "x")).isInstanceOf(IndexOutOfBoundsException.class);

            assertThat(array.capacity()).isEqualTo(2);
            assertThat(contentsOf(array)).containsExactly("a", "b");
        }

        @Test
        @DisplayName("the iterator's next() throws NoSuchElementException after the last element")
        void iteratorPastTheEnd() {
            Iterator<String> iterator = arrayOf("a").iterator();
            iterator.next();

            assertThatThrownBy(iterator::next).isInstanceOf(NoSuchElementException.class);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("matches ArrayList over 10,000 random operations")
        void matchesArrayList() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            var array = new DynamicArray<Integer>(1);
            List<Integer> model = new ArrayList<>();

            for (int step = 0; step < 10_000; step++) {
                int value = random.nextInt(100);
                switch (random.nextInt(6)) {
                    case 0, 1 -> {
                        array.add(value);
                        model.add(value);
                    }
                    case 2 -> {
                        int index = random.nextInt(model.size() + 1);
                        array.add(index, value);
                        model.add(index, value);
                    }
                    case 3 -> {
                        if (!model.isEmpty()) {
                            int index = random.nextInt(model.size());
                            assertThat(array.remove(index)).as("remove(%d) at step %d", index, step)
                                    .isEqualTo(model.remove(index));
                        }
                    }
                    case 4 -> {
                        if (!model.isEmpty()) {
                            int index = random.nextInt(model.size());
                            assertThat(array.set(index, value)).as("set(%d, %d) at step %d", index, value, step)
                                    .isEqualTo(model.set(index, value));
                        }
                    }
                    default -> assertThat(array.indexOf(value)).as("indexOf(%d) at step %d", value, step)
                            .isEqualTo(model.indexOf(value));
                }
                assertThat(contentsOf(array)).as("contents after step %d", step).isEqualTo(model);
            }
        }
    }
}
