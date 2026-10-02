package practice.j05.ex09;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final int capacity;
    private final Map<K, V> entries;

    public LruCache(int capacity) {
        // Validate first: an object should never exist in a state that breaks its rules.
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1, but was " + capacity);
        }
        this.capacity = capacity;
        // accessOrder = true: iteration goes from least to most recently *accessed* (get or put),
        // instead of insertion order. The JDK does the bookkeeping for us.
        this.entries = new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
                // LinkedHashMap calls this after inserting a new key. Returning true removes the eldest entry,
                // which in access order is the least recently used one.
                return size() >= LruCache.this.capacity;
            }
        };
    }

    public Optional<V> get(K key) {
        Objects.requireNonNull(key, "key");
        // Values are never null (put() rejects them), so a null here can only mean "absent".
        return Optional.ofNullable(entries.get(key));
    }

    public void put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");
        entries.put(key, value);
    }

    public int size() {
        return entries.size();
    }

    public int capacity() {
        return capacity;
    }

    public List<K> keys() {
        // List.copyOf makes an unmodifiable copy, so callers can neither change the cache through it
        // nor see later changes.
        return List.copyOf(entries.keySet());
    }
}
