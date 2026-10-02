package practice.j05.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.NoSuchElementException;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J05-03 · Stacks and queues")
class LinkedStackTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a new stack is empty")
        void newStackIsEmpty() {
            var stack = new LinkedStack<String>();

            assertThat(stack.size()).isZero();
            assertThat(stack.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("pop() returns the elements in last-in, first-out order")
        void popIsLastInFirstOut() {
            var stack = new LinkedStack<String>();
            stack.push("a");
            stack.push("b");
            stack.push("c");

            assertThat(stack.pop()).isEqualTo("c");
            assertThat(stack.pop()).isEqualTo("b");
            assertThat(stack.pop()).isEqualTo("a");
        }

        @Test
        @DisplayName("peek() returns the top element without removing it")
        void peekDoesNotRemove() {
            var stack = new LinkedStack<String>();
            stack.push("a");
            stack.push("b");

            assertThat(stack.peek()).isEqualTo("b");
            assertThat(stack.peek()).isEqualTo("b");
            assertThat(stack.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("size() goes up with push() and down with pop()")
        void sizeFollowsPushAndPop() {
            var stack = new LinkedStack<String>();
            stack.push("a");
            stack.push("b");
            stack.push("c");

            stack.pop();

            assertThat(stack.size()).isEqualTo(2);
            assertThat(stack.isEmpty()).isFalse();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a stack with one element is empty again after one pop()")
        void singleElement() {
            var stack = new LinkedStack<String>();
            stack.push("a");

            assertThat(stack.pop()).isEqualTo("a");

            assertThat(stack.isEmpty()).isTrue();
            assertThat(stack.size()).isZero();
        }

        @Test
        @DisplayName("keeps equal elements as separate entries")
        void keepsDuplicates() {
            var stack = new LinkedStack<String>();
            stack.push("x");
            stack.push("x");

            assertThat(stack.size()).isEqualTo(2);
            assertThat(stack.pop()).isEqualTo("x");
            assertThat(stack.pop()).isEqualTo("x");
            assertThat(stack.isEmpty()).isTrue();
        }

        @Test
        @DisplayName("pushes and pops can take turns")
        void interleaved() {
            var stack = new LinkedStack<Integer>();
            stack.push(1);
            stack.push(2);

            assertThat(stack.pop()).isEqualTo(2);
            stack.push(3);
            assertThat(stack.pop()).isEqualTo(3);
            assertThat(stack.pop()).isEqualTo(1);
        }

        @Test
        @DisplayName("works again after it has been emptied")
        void reusableAfterEmptied() {
            var stack = new LinkedStack<String>();
            stack.push("a");
            stack.pop();

            stack.push("b");

            assertThat(stack.peek()).isEqualTo("b");
            assertThat(stack.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("holds 100,000 elements and returns them in reverse order")
        void largeInput() {
            var stack = new LinkedStack<Integer>();
            for (int i = 0; i < 100_000; i++) {
                stack.push(i);
            }

            assertThat(stack.size()).isEqualTo(100_000);
            for (int i = 99_999; i >= 0; i--) {
                assertThat(stack.pop()).isEqualTo(i);
            }
            assertThat(stack.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("pop() on an empty stack throws NoSuchElementException")
        void popEmpty() {
            var stack = new LinkedStack<String>();

            assertThatThrownBy(stack::pop).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("peek() on an empty stack throws NoSuchElementException")
        void peekEmpty() {
            var stack = new LinkedStack<String>();

            assertThatThrownBy(stack::peek).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("pop() throws once every element has been popped")
        void popAfterEmptied() {
            var stack = new LinkedStack<String>();
            stack.push("a");
            stack.pop();

            assertThatThrownBy(stack::pop).isInstanceOf(NoSuchElementException.class);
        }

        @Test
        @DisplayName("a failed pop() leaves the stack empty and usable")
        void failedPopChangesNothing() {
            var stack = new LinkedStack<String>();

            assertThatThrownBy(stack::pop).isInstanceOf(NoSuchElementException.class);

            assertThat(stack.size()).isZero();
            stack.push("a");
            assertThat(stack.pop()).isEqualTo("a");
        }

        @Test
        @DisplayName("rejects null elements and stays unchanged")
        void rejectsNull() {
            var stack = new LinkedStack<String>();

            assertThatThrownBy(() -> stack.push(null)).isInstanceOf(NullPointerException.class);

            assertThat(stack.isEmpty()).isTrue();
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("matches ArrayDeque used as a stack over 10,000 random operations")
        void matchesArrayDeque() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            var stack = new LinkedStack<Integer>();
            Deque<Integer> model = new ArrayDeque<>();

            for (int step = 0; step < 10_000; step++) {
                switch (random.nextInt(3)) {
                    case 0 -> {
                        int value = random.nextInt(1_000);
                        stack.push(value);
                        model.push(value);
                    }
                    case 1 -> {
                        if (!model.isEmpty()) {
                            assertThat(stack.pop()).as("pop() at step %d", step).isEqualTo(model.pop());
                        }
                    }
                    default -> {
                        if (!model.isEmpty()) {
                            assertThat(stack.peek()).as("peek() at step %d", step).isEqualTo(model.peek());
                        }
                    }
                }
                assertThat(stack.size()).as("size() after step %d", step).isEqualTo(model.size());
                assertThat(stack.isEmpty()).as("isEmpty() after step %d", step).isEqualTo(model.isEmpty());
            }
        }
    }
}
