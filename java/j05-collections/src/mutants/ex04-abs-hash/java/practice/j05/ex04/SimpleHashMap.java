package practice.j05.ex04;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * A hash table with separate chaining: a small version of {@link java.util.HashMap}.
 *
 * <h2>How it stores entries</h2>
 * The map has an array of <em>buckets</em>. Each bucket holds a chain (a linked list) of the entries whose keys
 * belong in it. A key belongs in bucket {@code Math.floorMod(key.hashCode(), capacity())}, and keys are
 * compared with {@code equals()}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A new map has {@value #DEFAULT_CAPACITY} buckets, or as many as passed to the constructor. That number
 *       must be at least 1; otherwise the constructor throws {@link IllegalArgumentException}.</li>
 *   <li>When {@link #put} adds a new key and the size becomes greater than {@value #LOAD_FACTOR} &times; the
 *       capacity, the capacity doubles and every entry moves to its bucket in the new array ("rehashing").
 *       Replacing the value of an existing key never resizes, and removing never shrinks the capacity.</li>
 *   <li>{@link #put} returns the previous value, or empty if the key is new. {@link #get} and {@link #remove}
 *       return empty if the key isn't in the map.</li>
 *   <li>{@code null} keys and values are rejected with {@link NullPointerException}, and the map stays
 *       unchanged. Because values are never {@code null}, an empty {@link Optional} always means "no such
 *       key".</li>
 *   <li>{@link #keys()} returns an unmodifiable snapshot of the keys, in no particular order.</li>
 *   <li><strong>Keys must not change</strong> in a way that changes their {@code equals()} or {@code hashCode()}
 *       while they are in the map. If one does, the map can't find its entry anymore: {@link #get} looks in the
 *       wrong bucket, but the entry still counts in {@link #size()} and shows up in {@link #keys()}.
 *       {@link java.util.HashMap} behaves the same way.</li>
 * </ul>
 *
 * @param <K> key type
 * @param <V> value type
 */
public class SimpleHashMap<K, V> {

    /** The number of buckets of a map created with {@link #SimpleHashMap()}. */
    public static final int DEFAULT_CAPACITY = 16;

    /** The largest allowed ratio of entries to buckets before the map grows. */
    public static final double LOAD_FACTOR = 0.75;

    // One entry in a bucket's chain. Not a record: put() replaces the value of an existing entry in place,
    // and remove() and resize() relink `next`.
    private static final class Node<K, V> {
        private final K key;
        private V value;
        private Node<K, V> next;

        Node(K key, V value, Node<K, V> next) {
            this.key = key;
            this.value = value;
            this.next = next;
        }
    }

    private Node<K, V>[] buckets;
    private int size;

    /** Creates an empty map with {@value #DEFAULT_CAPACITY} buckets. */
    public SimpleHashMap() {
        this(DEFAULT_CAPACITY);
    }

    /** Creates an empty map with {@code initialCapacity} buckets. */
    public SimpleHashMap(int initialCapacity) {
        if (initialCapacity < 1) {
            throw new IllegalArgumentException("initialCapacity must be at least 1, but was " + initialCapacity);
        }
        this.buckets = newBuckets(initialCapacity);
    }

    /** Stores {@code value} under {@code key} and returns the value that was stored there before, if any. */
    public Optional<V> put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        Node<K, V> existing = find(key);
        if (existing != null) {
            V previous = existing.value;
            existing.value = value;
            return Optional.of(previous);
        }
        int index = indexFor(key, buckets.length);
        // Link the new entry at the front of the chain: O(1), and the order inside a chain doesn't matter.
        buckets[index] = new Node<>(key, value, buckets[index]);
        size++;
        if (size > buckets.length * LOAD_FACTOR) {
            resize();
        }
        return Optional.empty();
    }

    /** The value stored under {@code key}, or empty if there is none. */
    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "key");
        Node<K, V> node = find(key);
        return node == null ? Optional.empty() : Optional.of(node.value);
    }

    /** Whether the map has an entry for {@code key}. */
    public boolean containsKey(K key) {
        Objects.requireNonNull(key, "key");
        return find(key) != null;
    }

    /** Removes the entry for {@code key} and returns its value, or empty if there was none. */
    public Optional<V> remove(K key) {
        Objects.requireNonNull(key, "key");
        int index = indexFor(key, buckets.length);
        Node<K, V> previous = null;
        for (Node<K, V> node = buckets[index]; node != null; previous = node, node = node.next) {
            if (node.key.equals(key)) {
                // Unlink the node: whatever pointed to it now points past it, to the rest of the chain.
                if (previous == null) {
                    buckets[index] = node.next;
                } else {
                    previous.next = node.next;
                }
                size--;
                return Optional.of(node.value);
            }
        }
        return Optional.empty();
    }

    /** The number of entries. */
    public int size() {
        return size;
    }

    /** An unmodifiable snapshot of the keys, in no particular order. */
    public Set<K> keys() {
        List<K> keys = new ArrayList<>(size);
        for (Node<K, V> head : buckets) {
            for (Node<K, V> node = head; node != null; node = node.next) {
                keys.add(node.key);
            }
        }
        return Set.copyOf(keys);
    }

    /** The number of buckets. */
    public int capacity() {
        return buckets.length;
    }

    private Node<K, V> find(K key) {
        for (Node<K, V> node = buckets[indexFor(key, buckets.length)]; node != null; node = node.next) {
            // Compare with equals(), never ==: an equal key is often a different object, such as a String
            // built at runtime. (HashMap also stores each key's hash in its node and compares that first,
            // because equals() can be slow.)
            if (node.key.equals(key)) {
                return node;
            }
        }
        return null;
    }

    private void resize() {
        Node<K, V>[] old = buckets;
        buckets = newBuckets(old.length * 2);
        // A key's bucket depends on the capacity, so every entry must be placed again. Copying the old
        // array into the new one as it is would leave keys in buckets where get() never looks.
        for (Node<K, V> head : old) {
            Node<K, V> node = head;
            while (node != null) {
                Node<K, V> next = node.next; // remember it before relinking the node
                int index = indexFor(node.key, buckets.length);
                node.next = buckets[index];
                buckets[index] = node;
                node = next;
            }
        }
    }

    private static int indexFor(Object key, int capacity) {
        // hashCode() can be negative, so make it positive first.
        return Math.abs(key.hashCode()) % capacity;
    }

    @SuppressWarnings("unchecked") // Java can't create a generic array; this one only ever holds Node<K, V>.
    private static <K, V> Node<K, V>[] newBuckets(int capacity) {
        return (Node<K, V>[]) new Node<?, ?>[capacity];
    }
}
