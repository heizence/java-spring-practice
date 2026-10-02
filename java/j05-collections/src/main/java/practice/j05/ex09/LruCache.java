package practice.j05.ex09;

import java.util.List;
import java.util.Optional;

/**
 * A cache with a fixed capacity that evicts the <em>least recently used</em> entry when it is full.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@code capacity} must be at least 1. Otherwise the constructor throws {@link IllegalArgumentException}.</li>
 *   <li>Both {@link #get} (when the key is present) and {@link #put} count as <em>using</em> an entry.</li>
 *   <li>Putting a <em>new</em> key into a full cache first evicts the least recently used entry.</li>
 *   <li>Putting an <em>existing</em> key replaces its value and marks it as most recently used. Nothing is evicted.</li>
 *   <li>Getting a missing key returns {@link Optional#empty()} and changes nothing.</li>
 *   <li>{@code null} keys and values are rejected with {@link NullPointerException}.</li>
 *   <li>{@link #keys()} returns the keys from least to most recently used, as an unmodifiable snapshot:
 *       later changes to the cache don't show up in a list you already have.</li>
 * </ul>
 *
 * @param <K> key type
 * @param <V> value type
 */
public class LruCache<K, V> {

    public LruCache(int capacity) {
        // TODO: J05-09
        throw new UnsupportedOperationException("TODO: J05-09");
    }

    /** Returns the value stored for {@code key}, or empty if there is none. */
    public Optional<V> get(K key) {
        // TODO: J05-09
        throw new UnsupportedOperationException("TODO: J05-09");
    }

    /** Stores {@code value} under {@code key}, evicting the least recently used entry if needed. */
    public void put(K key, V value) {
        // TODO: J05-09
        throw new UnsupportedOperationException("TODO: J05-09");
    }

    /** Number of entries currently in the cache. */
    public int size() {
        // TODO: J05-09
        throw new UnsupportedOperationException("TODO: J05-09");
    }

    /** The capacity passed to the constructor. */
    public int capacity() {
        // TODO: J05-09
        throw new UnsupportedOperationException("TODO: J05-09");
    }

    /** The keys, from least to most recently used. */
    public List<K> keys() {
        // TODO: J05-09
        throw new UnsupportedOperationException("TODO: J05-09");
    }
}
