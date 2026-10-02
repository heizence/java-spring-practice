package practice.j05.ex05;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.TreeSet;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J05-05 · Binary search tree")
class BinarySearchTreeTest {

    /** A tree built by adding {@code keys} in the given order. The order decides the shape of the tree. */
    private static BinarySearchTree<Integer> treeOf(Integer... keys) {
        var tree = new BinarySearchTree<Integer>();
        for (Integer key : keys) {
            tree.add(key);
        }
        return tree;
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a new tree is empty")
        void newTreeIsEmpty() {
            var tree = new BinarySearchTree<Integer>();

            assertThat(tree.size()).isZero();
            assertThat(tree.inOrder()).isEmpty();
            assertThat(tree.height()).isZero();
        }

        @Test
        @DisplayName("add() returns true for a new key, and contains() finds it")
        void addAndContains() {
            var tree = new BinarySearchTree<Integer>();

            assertThat(tree.add(5)).isTrue();

            assertThat(tree.contains(5)).isTrue();
            assertThat(tree.contains(3)).isFalse();
            assertThat(tree.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("inOrder() returns the keys from smallest to largest")
        void inOrderIsSorted() {
            var tree = treeOf(5, 3, 8, 1, 4, 9);

            assertThat(tree.inOrder()).containsExactly(1, 3, 4, 5, 8, 9);
        }

        @Test
        @DisplayName("min() and max() return the smallest and the largest key")
        void minAndMax() {
            var tree = treeOf(5, 3, 8, 1, 4, 9);

            assertThat(tree.min()).contains(1);
            assertThat(tree.max()).contains(9);
        }

        @Test
        @DisplayName("floor() returns the key itself when the tree contains it")
        void floorOfExistingKey() {
            var tree = treeOf(10, 5, 15);

            assertThat(tree.floor(5)).contains(5);
            assertThat(tree.floor(15)).contains(15);
        }

        @Test
        @DisplayName("floor() returns the largest smaller key when the tree doesn't contain the key")
        void floorOfMissingKey() {
            var tree = treeOf(10, 5, 15);

            assertThat(tree.floor(7)).contains(5);
            assertThat(tree.floor(12)).contains(10);
            assertThat(tree.floor(20)).contains(15);
        }

        @Test
        @DisplayName("height() counts the nodes on the longest path: 3 for a balanced tree of 7 keys")
        void heightOfBalancedTree() {
            var tree = treeOf(4, 2, 6, 1, 3, 5, 7);

            assertThat(tree.height()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("add() ignores a key that is already in the tree and returns false")
        void ignoresDuplicates() {
            var tree = treeOf(5, 3);

            assertThat(tree.add(5)).isFalse();
            assertThat(tree.add(3)).isFalse();

            assertThat(tree.size()).isEqualTo(2);
            assertThat(tree.inOrder()).containsExactly(3, 5);
        }

        @Test
        @DisplayName("a tree with one key has height 1, and that key is both min and max")
        void singleKey() {
            var tree = treeOf(42);

            assertThat(tree.height()).isEqualTo(1);
            assertThat(tree.min()).contains(42);
            assertThat(tree.max()).contains(42);
            assertThat(tree.inOrder()).containsExactly(42);
        }

        @Test
        @DisplayName("an empty tree has no min, no max and no floor, and contains nothing")
        void emptyTree() {
            var tree = new BinarySearchTree<Integer>();

            assertThat(tree.min()).isEmpty();
            assertThat(tree.max()).isEmpty();
            assertThat(tree.floor(5)).isEmpty();
            assertThat(tree.contains(5)).isFalse();
        }

        @Test
        @DisplayName("floor() returns empty when every key is larger")
        void floorBelowSmallest() {
            var tree = treeOf(10, 20);

            assertThat(tree.floor(9)).isEmpty();
        }

        @Test
        @DisplayName("floor() goes back to the best candidate: in 10, 5, 15, 12 the floor of 11 is 10")
        void floorKeepsBestCandidate() {
            var tree = treeOf(10, 5, 15, 12);

            assertThat(tree.floor(11)).contains(10);
            assertThat(tree.floor(14)).contains(12);
        }

        @Test
        @DisplayName("keys added in sorted order make the tree as tall as its size, because it doesn't balance itself")
        void sortedInsertionDegenerates() {
            var tree = new BinarySearchTree<Integer>();

            for (int key = 1; key <= 1_000; key++) {
                tree.add(key);
            }

            assertThat(tree.height()).isEqualTo(1_000);
            assertThat(tree.min()).contains(1);
            assertThat(tree.max()).contains(1_000);
            assertThat(tree.floor(500)).contains(500);
        }

        @Test
        @DisplayName("orders negative keys and the extreme int values correctly")
        void negativeAndExtremeKeys() {
            var tree = treeOf(0, -5, Integer.MAX_VALUE, Integer.MIN_VALUE, 7);

            assertThat(tree.inOrder()).containsExactly(Integer.MIN_VALUE, -5, 0, 7, Integer.MAX_VALUE);
            assertThat(tree.floor(-1)).contains(-5);
        }

        @Test
        @DisplayName("orders String keys by compareTo, so uppercase letters come first")
        void stringKeys() {
            var tree = new BinarySearchTree<String>();
            tree.add("pear");
            tree.add("apple");
            tree.add("Banana");

            assertThat(tree.inOrder()).containsExactly("Banana", "apple", "pear");
            assertThat(tree.floor("orange")).contains("apple");
        }

        @Test
        @DisplayName("inOrder() is a snapshot that can't be modified")
        void inOrderIsUnmodifiableSnapshot() {
            var tree = treeOf(2, 1);

            List<Integer> before = tree.inOrder();
            tree.add(3);

            assertThat(before).containsExactly(1, 2);
            assertThatThrownBy(() -> before.add(9)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects null keys in add(), contains() and floor()")
        void rejectsNullKeys() {
            var tree = treeOf(5);

            assertThatThrownBy(() -> tree.add(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> tree.contains(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> tree.floor(null)).isInstanceOf(NullPointerException.class);
            assertThat(tree.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("rejects a null key even in an empty tree")
        void rejectsNullInEmptyTree() {
            var tree = new BinarySearchTree<Integer>();

            assertThatThrownBy(() -> tree.add(null)).isInstanceOf(NullPointerException.class);

            assertThat(tree.size()).isZero();
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("matches TreeSet over 10,000 random operations")
        void matchesTreeSet() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            var tree = new BinarySearchTree<Integer>();
            TreeSet<Integer> model = new TreeSet<>();

            for (int step = 0; step < 10_000; step++) {
                int key = random.nextInt(2_000) - 1_000;
                switch (random.nextInt(5)) {
                    case 0, 1 -> assertThat(tree.add(key)).as("add(%d) at step %d", key, step)
                            .isEqualTo(model.add(key));
                    case 2 -> assertThat(tree.contains(key)).as("contains(%d) at step %d", key, step)
                            .isEqualTo(model.contains(key));
                    case 3 -> assertThat(tree.floor(key)).as("floor(%d) at step %d", key, step)
                            .isEqualTo(Optional.ofNullable(model.floor(key)));
                    default -> {
                        assertThat(tree.min()).as("min() at step %d", step)
                                .isEqualTo(model.isEmpty() ? Optional.empty() : Optional.of(model.first()));
                        assertThat(tree.max()).as("max() at step %d", step)
                                .isEqualTo(model.isEmpty() ? Optional.empty() : Optional.of(model.last()));
                    }
                }
                assertThat(tree.size()).as("size() after step %d", step).isEqualTo(model.size());
                if (step % 1_000 == 0) {
                    assertThat(tree.inOrder()).as("inOrder() after step %d", step).isEqualTo(new ArrayList<>(model));
                }
            }
            assertThat(tree.inOrder()).isEqualTo(new ArrayList<>(model));
        }
    }
}
