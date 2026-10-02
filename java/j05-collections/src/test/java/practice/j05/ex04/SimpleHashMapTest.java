package practice.j05.ex04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J05-04 · Hash table")
class SimpleHashMapTest {

    /** Every instance has the same hash code, so all keys land in the same bucket. Equality uses the name. */
    record CollidingKey(String name) {
        @Override
        public int hashCode() {
            return 7;
        }
    }

    /** A key whose equals() and hashCode() change when its id changes. Never write map keys like this. */
    static final class MutableKey {
        int id;

        MutableKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof MutableKey key && key.id == id;
        }

        @Override
        public int hashCode() {
            return id;
        }
    }

    /** A map that holds the three colliding keys a, b and c, with the values 1, 2 and 3. */
    private static SimpleHashMap<CollidingKey, Integer> mapWithOneFullBucket() {
        var map = new SimpleHashMap<CollidingKey, Integer>();
        map.put(new CollidingKey("a"), 1);
        map.put(new CollidingKey("b"), 2);
        map.put(new CollidingKey("c"), 3);
        return map;
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a new map is empty")
        void newMapIsEmpty() {
            var map = new SimpleHashMap<String, Integer>();

            assertThat(map.size()).isZero();
            assertThat(map.keys()).isEmpty();
        }

        @Test
        @DisplayName("returns empty for a key that was never stored")
        void getMissingKey() {
            var map = new SimpleHashMap<String, Integer>();

            assertThat(map.get("apple")).isEmpty();
        }

        @Test
        @DisplayName("returns the value stored for a key")
        void getStoredValue() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);
            map.put("pear", 5);

            assertThat(map.get("apple")).contains(3);
            assertThat(map.get("pear")).contains(5);
            assertThat(map.size()).isEqualTo(2);
        }

        @Test
        @DisplayName("put() returns empty for a new key")
        void putNewKey() {
            var map = new SimpleHashMap<String, Integer>();

            assertThat(map.put("apple", 3)).isEmpty();
        }

        @Test
        @DisplayName("put() on an existing key replaces the value, returns the old one and keeps the size")
        void putExistingKey() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);

            Optional<Integer> previous = map.put("apple", 4);

            assertThat(previous).contains(3);
            assertThat(map.get("apple")).contains(4);
            assertThat(map.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("containsKey() tells whether a key is stored")
        void containsKey() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);

            assertThat(map.containsKey("apple")).isTrue();
            assertThat(map.containsKey("pear")).isFalse();
        }

        @Test
        @DisplayName("remove() returns the value and forgets the key")
        void removeKey() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);
            map.put("pear", 5);

            Optional<Integer> removed = map.remove("apple");

            assertThat(removed).contains(3);
            assertThat(map.get("apple")).isEmpty();
            assertThat(map.containsKey("apple")).isFalse();
            assertThat(map.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("remove() of a missing key returns empty and changes nothing")
        void removeMissingKey() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);

            assertThat(map.remove("pear")).isEmpty();

            assertThat(map.size()).isEqualTo(1);
            assertThat(map.get("apple")).contains(3);
        }

        @Test
        @DisplayName("keys() returns every key")
        void keys() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);
            map.put("pear", 5);
            map.put("plum", 7);

            assertThat(map.keys()).containsExactlyInAnyOrder("apple", "pear", "plum");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a new map has 16 buckets")
        void defaultCapacity() {
            assertThat(new SimpleHashMap<String, Integer>().capacity()).isEqualTo(16);
        }

        @ParameterizedTest(name = "{0} buckets")
        @ValueSource(ints = {1, 10})
        @DisplayName("a map has as many buckets as passed to the constructor")
        void customCapacity(int capacity) {
            assertThat(new SimpleHashMap<String, Integer>(capacity).capacity()).isEqualTo(capacity);
        }

        @Test
        @DisplayName("keeps entries apart when all keys land in the same bucket")
        void collidingKeys() {
            var map = mapWithOneFullBucket();

            assertThat(map.get(new CollidingKey("a"))).contains(1);
            assertThat(map.get(new CollidingKey("b"))).contains(2);
            assertThat(map.get(new CollidingKey("c"))).contains(3);
            assertThat(map.get(new CollidingKey("d"))).isEmpty();
            assertThat(map.size()).isEqualTo(3);
        }

        @ParameterizedTest(name = "remove {0}")
        @ValueSource(strings = {"a", "b", "c"})
        @DisplayName("removes any key from a shared bucket without losing the others")
        void removeFromSharedBucket(String name) {
            var map = mapWithOneFullBucket();

            map.remove(new CollidingKey(name));

            assertThat(map.containsKey(new CollidingKey(name))).isFalse();
            assertThat(map.size()).isEqualTo(2);
            assertThat(map.keys()).hasSize(2);
            for (String other : Set.of("a", "b", "c")) {
                if (!other.equals(name)) {
                    assertThat(map.containsKey(new CollidingKey(other))).as("still contains %s", other).isTrue();
                }
            }
        }

        @Test
        @DisplayName("replaces the value of a key in a shared bucket")
        void replaceInSharedBucket() {
            var map = mapWithOneFullBucket();

            Optional<Integer> previous = map.put(new CollidingKey("b"), 20);

            assertThat(previous).contains(2);
            assertThat(map.get(new CollidingKey("b"))).contains(20);
            assertThat(map.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("finds a key by equals(), not by being the same object")
        void comparesKeysWithEquals() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);
            String sameText = new String("apple"); // equal, but a different object

            assertThat(map.get(sameText)).contains(3);
            assertThat(map.containsKey(sameText)).isTrue();
        }

        @Test
        @DisplayName("keeps 16 buckets for 12 keys, the most that load factor 0.75 allows")
        void doesNotGrowAtTwelve() {
            var map = new SimpleHashMap<Integer, String>();

            for (int key = 0; key < 12; key++) {
                map.put(key, "v" + key);
            }

            assertThat(map.capacity()).isEqualTo(16);
        }

        @Test
        @DisplayName("doubles to 32 buckets when the 13th key arrives")
        void doublesAtThirteen() {
            var map = new SimpleHashMap<Integer, String>();

            for (int key = 0; key < 13; key++) {
                map.put(key, "v" + key);
            }

            assertThat(map.capacity()).isEqualTo(32);
        }

        @Test
        @DisplayName("finds every key after the 13th key makes the map grow")
        void findsKeysAfterResize() {
            var map = new SimpleHashMap<Integer, String>();

            for (int key = 16; key < 29; key++) { // keys 16 to 28 move to other buckets when 16 becomes 32
                map.put(key, "v" + key);
            }

            for (int key = 16; key < 29; key++) {
                assertThat(map.get(key)).as("get(%d)", key).contains("v" + key);
            }
        }

        @Test
        @DisplayName("a map with 1 bucket grows on its first key")
        void oneBucketGrowsAtOnce() {
            var map = new SimpleHashMap<String, Integer>(1);

            map.put("apple", 3);

            assertThat(map.capacity()).isEqualTo(2);
            assertThat(map.get("apple")).contains(3);
        }

        @Test
        @DisplayName("replacing a value never makes the map grow")
        void replacingDoesNotGrow() {
            var map = new SimpleHashMap<String, Integer>(4);
            map.put("a", 1);
            map.put("b", 2);
            map.put("c", 3); // 3 entries in 4 buckets: exactly at the limit

            map.put("c", 30);

            assertThat(map.capacity()).isEqualTo(4);
            assertThat(map.size()).isEqualTo(3);
        }

        @Test
        @DisplayName("grows from 1 to 2,048 buckets for 1,000 keys and still finds them all")
        void manyResizes() {
            var map = new SimpleHashMap<Integer, Integer>(1);

            for (int key = 0; key < 1_000; key++) {
                map.put(key, key * 10);
            }

            assertThat(map.capacity()).isEqualTo(2_048);
            assertThat(map.size()).isEqualTo(1_000);
            for (int key = 0; key < 1_000; key++) {
                assertThat(map.get(key)).as("get(%d)", key).contains(key * 10);
            }
        }

        @Test
        @DisplayName("removing keys never shrinks the capacity")
        void removingDoesNotShrink() {
            var map = new SimpleHashMap<Integer, String>();
            for (int key = 0; key < 13; key++) {
                map.put(key, "v" + key);
            }

            for (int key = 0; key < 13; key++) {
                map.remove(key);
            }

            assertThat(map.capacity()).isEqualTo(32);
            assertThat(map.size()).isZero();
        }

        @Test
        @DisplayName("handles negative hash codes, including Integer.MIN_VALUE")
        void negativeHashCodes() {
            var map = new SimpleHashMap<Integer, String>(10); // an Integer's hash code is its value

            map.put(-1, "minus one");
            map.put(-17, "minus seventeen");
            map.put(Integer.MIN_VALUE, "min");
            map.put(Integer.MAX_VALUE, "max");

            assertThat(map.get(-1)).contains("minus one");
            assertThat(map.get(-17)).contains("minus seventeen");
            assertThat(map.get(Integer.MIN_VALUE)).contains("min");
            assertThat(map.get(Integer.MAX_VALUE)).contains("max");
        }

        @Test
        @DisplayName("a removed key can be stored again")
        void putAfterRemove() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);
            map.remove("apple");

            assertThat(map.put("apple", 4)).isEmpty();

            assertThat(map.get("apple")).contains(4);
            assertThat(map.size()).isEqualTo(1);
        }

        @Test
        @DisplayName("keys() is a snapshot that later changes don't affect")
        void keysIsSnapshot() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);

            Set<String> before = map.keys();
            map.put("pear", 5);

            assertThat(before).containsExactly("apple");
        }

        @Test
        @DisplayName("keys() can't be modified by the caller")
        void keysIsUnmodifiable() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);

            assertThatThrownBy(() -> map.keys().add("pear")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("can't find an entry after its key changed hash code (the mutable-key trap)")
        void mutableKeyTrap() {
            var map = new SimpleHashMap<MutableKey, String>(); // 16 buckets: id 1 → bucket 1, id 2 → bucket 2
            var key = new MutableKey(1);
            map.put(key, "one");

            key.id = 2; // changes equals() and hashCode() while the key is in the map

            assertThat(map.get(key)).isEmpty(); // looks in bucket 2, which is empty
            assertThat(map.containsKey(new MutableKey(1))).isFalse(); // bucket 1 holds a key that now equals 2
            assertThat(map.size()).isEqualTo(1); // the entry is still there, but nothing can reach it
            assertThat(map.keys()).containsExactly(key);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "capacity {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("rejects an initial capacity below 1")
        void rejectsCapacityBelowOne(int capacity) {
            assertThatThrownBy(() -> new SimpleHashMap<String, Integer>(capacity))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects null keys in put(), get(), containsKey() and remove()")
        void rejectsNullKeys() {
            var map = new SimpleHashMap<String, Integer>();

            assertThatThrownBy(() -> map.put(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.get(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.containsKey(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.remove(null)).isInstanceOf(NullPointerException.class);
            assertThat(map.size()).isZero();
        }

        @Test
        @DisplayName("rejects a null value and stores nothing")
        void rejectsNullValue() {
            var map = new SimpleHashMap<String, Integer>();

            assertThatThrownBy(() -> map.put("apple", null)).isInstanceOf(NullPointerException.class);

            assertThat(map.containsKey("apple")).isFalse();
            assertThat(map.size()).isZero();
        }

        @Test
        @DisplayName("a rejected null value doesn't replace the existing value")
        void rejectedNullKeepsOldValue() {
            var map = new SimpleHashMap<String, Integer>();
            map.put("apple", 3);

            assertThatThrownBy(() -> map.put("apple", null)).isInstanceOf(NullPointerException.class);

            assertThat(map.get("apple")).contains(3);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("matches HashMap over 20,000 random operations")
        void matchesHashMap() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            var map = new SimpleHashMap<Integer, Integer>(1);
            Map<Integer, Integer> model = new HashMap<>();

            for (int step = 0; step < 20_000; step++) {
                int key = random.nextInt(600) - 300; // negative keys have negative hash codes
                int operation = random.nextInt(10);
                if (operation < 5) {
                    int value = random.nextInt(1_000);
                    assertThat(map.put(key, value)).as("put(%d, %d) at step %d", key, value, step)
                            .isEqualTo(Optional.ofNullable(model.put(key, value)));
                } else if (operation < 7) {
                    assertThat(map.remove(key)).as("remove(%d) at step %d", key, step)
                            .isEqualTo(Optional.ofNullable(model.remove(key)));
                } else if (operation < 9) {
                    assertThat(map.get(key)).as("get(%d) at step %d", key, step)
                            .isEqualTo(Optional.ofNullable(model.get(key)));
                } else {
                    assertThat(map.containsKey(key)).as("containsKey(%d) at step %d", key, step)
                            .isEqualTo(model.containsKey(key));
                }
                assertThat(map.size()).as("size() after step %d", step).isEqualTo(model.size());
                if (step % 500 == 0) {
                    assertThat(map.keys()).as("keys() after step %d", step).isEqualTo(model.keySet());
                }
            }
            assertThat(map.keys()).isEqualTo(model.keySet());
        }
    }
}
