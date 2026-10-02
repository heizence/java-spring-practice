package practice.j05.ex05;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
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
 *   <li>{@link #min()} and {@link #max()} return empty for an empty tree.</li>
 *   <li>{@link #floor} returns the largest key that is less than or equal to the given key, or empty if every
 *       key is larger.</li>
 *   <li>{@link #inOrder()} returns all keys from smallest to largest, as an unmodifiable snapshot: keys added
 *       later don't show up in a list you already have.</li>
 *   <li>{@link #height()} is the number of nodes on the longest path from the root down to a leaf: 0 for an
 *       empty tree, 1 for a tree with one node.</li>
 *   <li>{@code null} keys are rejected with {@link NullPointerException}.</li>
 * </ul>
 *
 * @param <K> key type
 */
public class BinarySearchTree<K extends Comparable<K>> {

    private static final class Node<K> {
        private final K key;
        private Node<K> left;
        private Node<K> right;

        Node(K key) {
            this.key = key;
        }
    }

    private Node<K> root; // null when the tree is empty
    private int size;

    /** Adds {@code key} unless the tree already contains it. Returns whether the tree changed. */
    public boolean add(K key) {
        Objects.requireNonNull(key, "key");
        int sizeBefore = size;
        root = add(root, key);
        return size > sizeBefore;
    }

    // Recursion fits trees well: a tree is a node with two smaller trees. This method adds the key to the
    // subtree that starts at `node` and returns that subtree's root, which is new only if `node` was null.
    private Node<K> add(Node<K> node, K key) {
        if (node == null) {
            size++;
            return new Node<>(key);
        }
        int comparison = key.compareTo(node.key);
        if (comparison <= 0) {
            node.left = add(node.left, key);
        } else {
            node.right = add(node.right, key);
        }
        return node;
    }

    /** Whether the tree contains {@code key}. */
    public boolean contains(K key) {
        Objects.requireNonNull(key, "key");
        // A search follows one path down, so a loop is as clear as recursion here.
        Node<K> node = root;
        while (node != null) {
            int comparison = key.compareTo(node.key);
            if (comparison == 0) {
                return true;
            }
            node = comparison < 0 ? node.left : node.right;
        }
        return false;
    }

    /** The number of keys. */
    public int size() {
        return size;
    }

    /** The smallest key, or empty if the tree is empty. */
    public Optional<K> min() {
        if (root == null) {
            return Optional.empty();
        }
        Node<K> node = root;
        while (node.left != null) { // the smallest key is as far left as you can go
            node = node.left;
        }
        return Optional.of(node.key);
    }

    /** The largest key, or empty if the tree is empty. */
    public Optional<K> max() {
        if (root == null) {
            return Optional.empty();
        }
        Node<K> node = root;
        while (node.right != null) {
            node = node.right;
        }
        return Optional.of(node.key);
    }

    /** The largest key that is less than or equal to {@code key}, or empty if there is none. */
    public Optional<K> floor(K key) {
        Objects.requireNonNull(key, "key");
        K best = null; // the largest key <= `key` seen so far
        Node<K> node = root;
        while (node != null) {
            int comparison = key.compareTo(node.key);
            if (comparison == 0) {
                return Optional.of(node.key); // an exact match is the best possible answer
            }
            if (comparison < 0) {
                node = node.left; // node.key is too large, and so is everything to its right
            } else {
                best = node.key; // a candidate, but a larger one may still be to its right
                node = node.right;
            }
        }
        return Optional.ofNullable(best);
    }

    /** All keys, from smallest to largest. */
    public List<K> inOrder() {
        List<K> keys = new ArrayList<>(size);
        inOrder(root, keys);
        // Nobody else has a reference to `keys`, so a read-only view is as safe as a copy, and cheaper.
        return Collections.unmodifiableList(keys);
    }

    // In-order traversal: everything smaller (left), then the node itself, then everything larger (right).
    private static <K> void inOrder(Node<K> node, List<K> keys) {
        if (node == null) {
            return;
        }
        inOrder(node.left, keys);
        keys.add(node.key);
        inOrder(node.right, keys);
    }

    /** The number of nodes on the longest path from the root to a leaf; 0 for an empty tree. */
    public int height() {
        return height(root);
    }

    private static int height(Node<?> node) {
        if (node == null) {
            return 0; // base case: an empty tree
        }
        return 1 + Math.max(height(node.left), height(node.right));
    }
}
