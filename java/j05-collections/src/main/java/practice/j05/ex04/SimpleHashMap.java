package practice.j05.ex04;

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

    /** Creates an empty map with {@value #DEFAULT_CAPACITY} buckets. */
    public SimpleHashMap() {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }

    /** Creates an empty map with {@code initialCapacity} buckets. */
    public SimpleHashMap(int initialCapacity) {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }

    /** Stores {@code value} under {@code key} and returns the value that was stored there before, if any. */
    public Optional<V> put(K key, V value) {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }

    /** The value stored under {@code key}, or empty if there is none. */
    public Optional<V> get(K key) {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }

    /** Whether the map has an entry for {@code key}. */
    public boolean containsKey(K key) {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }

    /** Removes the entry for {@code key} and returns its value, or empty if there was none. */
    public Optional<V> remove(K key) {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }

    /** The number of entries. */
    public int size() {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }

    /** An unmodifiable snapshot of the keys, in no particular order. */
    public Set<K> keys() {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }

    /** The number of buckets. */
    public int capacity() {
        // TODO: J05-04
        throw new UnsupportedOperationException("TODO: J05-04");
    }
}
