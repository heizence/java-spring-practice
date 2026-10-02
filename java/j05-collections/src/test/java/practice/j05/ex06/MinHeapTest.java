package practice.j05.ex06;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.PriorityQueue;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J05-06 · Heap and priority queue")
class MinHeapTest {

    /** A heap with {@code elements} offered in the given order. */
    private static MinHeap<Integer> heapOf(Integer... elements) {
        var heap = new MinHeap<Integer>();
        for (Integer element : elements) {
            heap.offer(element);
        }
        return heap;
    }

    /** Polls every element, in the order the heap hands them out. */
    private static <E extends Comparable<? super E>> List<E> pollAll(MinHeap<E> heap) {
        List<E> polled = new ArrayList<>();
        while (!heap.isEmpty()) {
            polled.add(heap.poll());
        }
        return polled;
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a new heap is empty")
        void newHeapIsEmpty() {
            var heap = new MinHeap<Integer>();

            assertThat(heap.size()).isZero();
            assertThat(heap.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("peek() returns the smallest element without removing it")
        void peekReturnsSmallest() {
            var heap = heapOf(5, 1, 3);

            assertThat(heap.peek()).isEqualTo(1);
            assertThat(heap.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("poll() returns the elements from smallest to largest")
        void pollInAscendingOrder() {
            var heap = heapOf(5, 1, 4, 2, 3);

            assertThat(pollAll(heap)).containsExactly(1, 2, 3, 4, 5);
        }

        @Test
        @DisplayName("size() goes up with offer() and down with poll()")
        void sizeFollowsOfferAndPoll() {
            var heap = heapOf(3, 1, 2);

            heap.poll();

            assertThat(heap.size()).isEqualTo(2);
            assertThat(heap.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("keeps equal elements")
        void keepsDuplicates() {
            var heap = heapOf(2, 1, 2, 1);

            assertThat(pollAll(heap)).containsExactly(1, 1, 2, 2);
        }

        @Test
        @DisplayName("a heap with one element is empty after one poll()")
        void singleElement() {
            var heap = heapOf(7);

            assertThat(heap.poll()).isEqualTo(7);

            assertThat(heap.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("offers and polls can take turns")
        void interleaved() {
            var heap = heapOf(5, 3);

            assertThat(heap.poll()).isEqualTo(3);
            heap.offer(1);
            assertThat(heap.poll()).isEqualTo(1);
            heap.offer(4);
            assertThat(heap.poll()).isEqualTo(4);
            assertThat(heap.poll()).isEqualTo(5);
        }

        @Test
        @DisplayName("handles elements offered from largest to smallest, so each new one rises to the top")
        void descendingInput() {
            var heap = heapOf(10, 9, 8, 7, 6, 5, 4, 3, 2, 1);

            assertThat(pollAll(heap)).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }

        @Test
        @DisplayName("handles elements offered from smallest to largest")
        void ascendingInput() {
            var heap = heapOf(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

            assertThat(pollAll(heap)).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
        }

        @Test
        @DisplayName("orders negative numbers and the extreme int values correctly")
        void negativeAndExtremeValues() {
            var heap = heapOf(0, Integer.MAX_VALUE, -3, Integer.MIN_VALUE, 3);

            assertThat(pollAll(heap)).containsExactly(Integer.MIN_VALUE, -3, 0, 3, Integer.MAX_VALUE);
        }

        @Test
        @DisplayName("orders String elements by compareTo")
        void stringElements() {
            var heap = new MinHeap<String>();
            heap.offer("pear");
            heap.offer("apple");
            heap.offer("Banana");

            assertThat(pollAll(heap)).containsExactly("Banana", "apple", "pear");
        }

        @Test
        @DisplayName("sorts 100,000 random numbers (this is heap sort)")
        void largeInput() {
            var random = new Random(42);
            var heap = new MinHeap<Integer>();
            List<Integer> numbers = new ArrayList<>();
            for (int i = 0; i < 100_000; i++) {
                int number = random.nextInt();
                numbers.add(number);
                heap.offer(number);
            }

            List<Integer> polled = pollAll(heap);

            numbers.sort(null); // null means natural order
            assertThat(polled).isEqualTo(numbers);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("poll() on an empty heap throws NoSuchElementException")
        void pollEmpty() {
            var heap = new MinHeap<Integer>();

            assertThatThrownBy(heap::poll).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("peek() on an empty heap throws NoSuchElementException")
        void peekEmpty() {
            var heap = new MinHeap<Integer>();

            assertThatThrownBy(heap::peek).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("a failed poll() leaves the heap empty and usable")
        void failedPollChangesNothing() {
            var heap = heapOf(1);
            heap.poll();

            assertThatThrownBy(heap::poll).isInstanceOf(NoSuchElementException.class);

            assertThat(heap.size()).isZero();
            heap.offer(2);
            assertThat(heap.poll()).isEqualTo(2);
        }

        @Test
        @DisplayName("rejects null elements and stays unchanged")
        void rejectsNull() {
            var heap = heapOf(1);

            assertThatThrownBy(() -> heap.offer(null)).isInstanceOf(NullPointerException.class);

            assertThat(heap.size()).isEqualTo(1);
            assertThat(heap.peek()).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("matches PriorityQueue over 10,000 random operations")
        void matchesPriorityQueue() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            var heap = new MinHeap<Integer>();
            PriorityQueue<Integer> model = new PriorityQueue<>();

            for (int step = 0; step < 10_000; step++) {
                switch (random.nextInt(4)) {
                    case 0, 1 -> {
                        int value = random.nextInt(200);
                        heap.offer(value);
                        model.offer(value);
                    }
                    case 2 -> {
                        if (!model.isEmpty()) {
                            assertThat(heap.poll()).as("poll() at step %d", step).isEqualTo(model.poll());
                        }
                    }
                    default -> {
                        if (!model.isEmpty()) {
                            assertThat(heap.peek()).as("peek() at step %d", step).isEqualTo(model.peek());
                        }
                    }
                }
                assertThat(heap.size()).as("size() after step %d", step).isEqualTo(model.size());
            }
        }
    }
}
