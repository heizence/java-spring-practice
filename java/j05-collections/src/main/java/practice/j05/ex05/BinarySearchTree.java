package practice.j05.ex05;

import java.util.List;
import java.util.Optional;

/**
 * A set of keys stored in a binary search tree (BST), the idea behind {@link java.util.TreeSet} and
 * {@link java.util.TreeMap}.
 *
 * <p>Every node has up to two children. All keys in a node's left subtree are smaller than the node's key,
 * and all keys in its right subtree are larger. So a search can skip half of the remaining tree at each step
 * as long as the tree stays balanced. This tree doesn't balance itself (TreeMap does).
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Keys are ordered by their {@code compareTo}. {@link #add} ignores a key that is already in the tree
 *       and returns {@code false}; otherwise it returns {@code true}.</li>
 *   <li>{@link #min()} returns empty for an empty tree.</li>
 *   <li>{@link #floor} returns the largest key that is less than or equal to the given key, or empty if every
 *       key is larger.</li>
 *   <li>{@link #inOrder()} returns all keys from smallest to largest, as an unmodifiable snapshot: keys added
 *       later don't show up in a list you already have.</li>
 *   <li>{@code null} keys are rejected with {@link NullPointerException}.</li>
 * </ul>
 *
 * @param <K> key type
 */
public class BinarySearchTree<K extends Comparable<K>> {

    /** Adds {@code key} unless the tree already contains it. Returns whether the tree changed. */
    public boolean add(K key) {
        // TODO: J05-05
        throw new UnsupportedOperationException("TODO: J05-05");
    }

    /** Whether the tree contains {@code key}. */
    public boolean contains(K key) {
        // TODO: J05-05
        throw new UnsupportedOperationException("TODO: J05-05");
    }

    /** The number of keys. */
    public int size() {
        // TODO: J05-05
        throw new UnsupportedOperationException("TODO: J05-05");
    }

    /** The smallest key, or empty if the tree is empty. */
    public Optional<K> min() {
        // TODO: J05-05
        throw new UnsupportedOperationException("TODO: J05-05");
    }

    /** The largest key that is less than or equal to {@code key}, or empty if there is none. */
    public Optional<K> floor(K key) {
        // TODO: J05-05
        throw new UnsupportedOperationException("TODO: J05-05");
    }

    /** All keys, from smallest to largest. */
    public List<K> inOrder() {
        // TODO: J05-05
        throw new UnsupportedOperationException("TODO: J05-05");
    }
}
