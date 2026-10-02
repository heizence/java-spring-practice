package practice.j05.ex09;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J05-09 · LRU cache")
class LruCacheTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("returns the value stored for a key")
        void returnsStoredValue() {
            var cache = new LruCache<String, Integer>(2);

            cache.put("a", 1);

            assertThat(cache.get("a")).contains(1);
        }

        @Test
        @DisplayName("returns empty for a key that was never stored")
        void returnsEmptyForMissingKey() {
            var cache = new LruCache<String, Integer>(2);

            assertThat(cache.get("missing")).isEmpty();
        }

        @Test
        @DisplayName("reports its size and capacity")
        void reportsSizeAndCapacity() {
            var cache = new LruCache<String, Integer>(3);
            cache.put("a", 1);
            cache.put("b", 2);

            assertThat(cache.size()).isEqualTo(2);
            assertThat(cache.capacity()).isEqualTo(3);
        }

        @Test
        @DisplayName("evicts the least recently used entry when a new key arrives in a full cache")
        void evictsLeastRecentlyUsed() {
            var cache = new LruCache<String, Integer>(2);
            cache.put("a", 1);
            cache.put("b", 2);

            cache.put("c", 3);

            assertThat(cache.get("a")).isEmpty();
            assertThat(cache.keys()).containsExactly("b", "c");
            assertThat(cache.size()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("get() marks an entry as recently used, so it survives the next eviction")
        void getRefreshesRecency() {
            var cache = new LruCache<String, Integer>(2);
            cache.put("a", 1);
            cache.put("b", 2);

            cache.get("a");
            cache.put("c", 3);

            assertThat(cache.keys()).containsExactly("a", "c");
        }

        @Test
        @DisplayName("put() on an existing key replaces the value without evicting anything")
        void putExistingKeyEvictsNothing() {
            var cache = new LruCache<String, Integer>(2);
            cache.put("a", 1);
            cache.put("b", 2);

            cache.put("b", 20);

            assertThat(cache.keys()).containsExactly("a", "b");
            assertThat(cache.get("b")).contains(20);
            assertThat(cache.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("put() on an existing key marks it as recently used")
        void putExistingKeyRefreshesRecency() {
            var cache = new LruCache<String, Integer>(2);
            cache.put("a", 1);
            cache.put("b", 2);

            cache.put("a", 10);
            cache.put("c", 3);

            assertThat(cache.keys()).containsExactly("a", "c");
        }

        @Test
        @DisplayName("get() on a missing key changes nothing")
        void getMissingKeyChangesNothing() {
            var cache = new LruCache<String, Integer>(2);
            cache.put("a", 1);
            cache.put("b", 2);

            cache.get("missing");

            assertThat(cache.keys()).containsExactly("a", "b");
            assertThat(cache.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("a cache with capacity 1 keeps only the latest entry")
        void capacityOneKeepsLatest() {
            var cache = new LruCache<String, Integer>(1);

            cache.put("a", 1);
            cache.put("b", 2);

            assertThat(cache.keys()).containsExactly("b");
            assertThat(cache.get("a")).isEmpty();
        }

        @Test
        @DisplayName("keys() is a snapshot that later changes don't affect")
        void keysIsSnapshot() {
            var cache = new LruCache<String, Integer>(2);
            cache.put("a", 1);

            List<String> before = cache.keys();
            cache.put("b", 2);

            assertThat(before).containsExactly("a");
        }

        @Test
        @DisplayName("keys() can't be modified by the caller")
        void keysIsUnmodifiable() {
            var cache = new LruCache<String, Integer>(2);
            cache.put("a", 1);

            assertThatThrownBy(() -> cache.keys().add("x"))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "capacity {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("rejects a capacity below 1")
        void rejectsInvalidCapacity(int capacity) {
            assertThatThrownBy(() -> new LruCache<String, Integer>(capacity))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null keys in put() and get()")
        void rejectsNullKeys() {
            var cache = new LruCache<String, Integer>(2);

            assertThatThrownBy(() -> cache.put(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> cache.get(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects null values")
        void rejectsNullValues() {
            var cache = new LruCache<String, Integer>(2);

            assertThatThrownBy(() -> cache.put("a", null)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("matches a simple reference model over 10,000 random operations")
        void matchesReferenceModel() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            var cache = new LruCache<Integer, Integer>(5);
            List<Integer> order = new ArrayList<>(); // keys from least to most recently used
            Map<Integer, Integer> values = new HashMap<>();

            for (int step = 0; step < 10_000; step++) {
                int key = random.nextInt(10);
                if (random.nextBoolean()) {
                    int value = random.nextInt(1_000);
                    cache.put(key, value);
                    order.remove(Integer.valueOf(key));
                    order.add(key);
                    values.put(key, value);
                    if (order.size() > 5) {
                        values.remove(order.removeFirst());
                    }
                } else {
                    Optional<Integer> expected = Optional.ofNullable(values.get(key));
                    if (expected.isPresent()) {
                        order.remove(Integer.valueOf(key));
                        order.add(key);
                    }
                    assertThat(cache.get(key)).as("get(%d) at step %d", key, step).isEqualTo(expected);
                }
                assertThat(cache.keys()).as("keys() after step %d", step).isEqualTo(order);
            }
        }
    }
}
