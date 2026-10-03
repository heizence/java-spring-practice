package practice.j04.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.testkit.Predict;

@DisplayName("J04-03 · Type erasure and type tokens")
class TypedMapTest {

    private final TypedMap map = new TypedMap();

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("returns the value stored for a type")
        void storesAndReturns() {
            map.put(String.class, "Minji");

            assertThat(map.get(String.class)).contains("Minji");
        }

        @Test
        @DisplayName("keeps values of different types side by side")
        void differentTypes() {
            map.put(String.class, "Minji");
            map.put(Integer.class, 42);
            map.put(LocalDate.class, LocalDate.of(2026, 3, 1));

            assertThat(map.get(String.class)).contains("Minji");
            assertThat(map.get(Integer.class)).contains(42);
            assertThat(map.get(LocalDate.class)).contains(LocalDate.of(2026, 3, 1));
        }

        @Test
        @DisplayName("returns empty for a type that was never stored")
        void missingType() {
            assertThat(map.get(String.class)).isEmpty();
        }

        @Test
        @DisplayName("put() replaces the earlier value of the same type")
        void replaces() {
            map.put(String.class, "Minji");

            map.put(String.class, "Jisoo");

            assertThat(map.get(String.class)).contains("Jisoo");
            assertThat(map.types()).containsExactly(String.class);
        }

        @Test
        @DisplayName("contains() tells whether a type has a value")
        void contains() {
            map.put(String.class, "Minji");

            assertThat(map.contains(String.class)).isTrue();
            assertThat(map.contains(Integer.class)).isFalse();
        }

        @Test
        @DisplayName("remove() returns the removed value and forgets the type")
        void removes() {
            map.put(String.class, "Minji");

            assertThat(map.remove(String.class)).contains("Minji");
            assertThat(map.contains(String.class)).isFalse();
            assertThat(map.get(String.class)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a value stored under Number.class isn't found with Integer.class")
        void exactKeysSupertype() {
            map.put(Number.class, 42);

            assertThat(map.get(Integer.class)).isEmpty();
            assertThat(map.get(Number.class)).contains(42);
        }

        @Test
        @DisplayName("a value stored under Integer.class isn't found with Number.class")
        void exactKeysSubtype() {
            map.put(Integer.class, 42);

            assertThat(map.get(Number.class)).isEmpty();
            assertThat(map.contains(Number.class)).isFalse();
        }

        @Test
        @DisplayName("an interface type accepts any value that implements it")
        void interfaceKey() {
            var text = new StringBuilder("draft");

            map.put(CharSequence.class, text);

            assertThat(map.get(CharSequence.class)).containsSame(text);
        }

        @Test
        @DisplayName("accepts array types such as int[].class, which aren't primitive")
        void arrayKey() {
            int[] scores = {90, 85};

            map.put(int[].class, scores);

            assertThat(map.get(int[].class)).containsSame(scores);
        }

        @Test
        @DisplayName("a List stored under List.class comes back as a List, without its element type")
        void genericValue() {
            map.put(List.class, List.of("a", "b"));

            assertThat(map.get(List.class)).contains(List.of("a", "b"));
        }

        @Test
        @DisplayName("remove() of a type that was never stored returns empty and changes nothing")
        void removeMissing() {
            map.put(String.class, "Minji");

            assertThat(map.remove(Integer.class)).isEmpty();
            assertThat(map.types()).containsExactly(String.class);
        }

        @Test
        @DisplayName("types() lists every type that has a value")
        void types() {
            map.put(String.class, "Minji");
            map.put(Integer.class, 42);

            assertThat(map.types()).containsExactlyInAnyOrder(String.class, Integer.class);
        }

        @Test
        @DisplayName("types() of an empty map is empty")
        void typesOfEmptyMap() {
            assertThat(map.types()).isEmpty();
        }

        @Test
        @DisplayName("types() is a snapshot that later changes don't affect")
        void typesIsSnapshot() {
            map.put(String.class, "Minji");
            Set<Class<?>> before = map.types();

            map.put(Integer.class, 42);
            map.remove(String.class);

            assertThat(before).containsExactly(String.class);
        }

        @Test
        @DisplayName("types() can't be modified")
        void typesIsUnmodifiable() {
            map.put(String.class, "Minji");

            assertThatThrownBy(() -> map.types().add(Integer.class))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("put() rejects a value of the wrong type smuggled in with a raw Class, and stays unchanged")
        @SuppressWarnings({"rawtypes", "unchecked"})
        void rejectsWrongTypeThroughRawClass() {
            Class raw = Integer.class; // a raw Class: the compiler can't check what we put in

            assertThatThrownBy(() -> map.put(raw, "not a number")).isInstanceOf(ClassCastException.class);
            assertThat(map.contains(Integer.class)).isFalse();
        }

        @Test
        @DisplayName("every method rejects a null type")
        void rejectsNullType() {
            assertThatThrownBy(() -> map.put(null, "x")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.get(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.contains(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> map.remove(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("put() rejects a null value")
        void rejectsNullValue() {
            assertThatThrownBy(() -> map.put(String.class, null)).isInstanceOf(NullPointerException.class);
            assertThat(map.types()).isEmpty();
        }

        @Test
        @DisplayName("every method rejects a primitive type such as int.class")
        void rejectsPrimitiveTypes() {
            assertThatThrownBy(() -> map.put(int.class, 42)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> map.get(int.class)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> map.contains(boolean.class)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> map.remove(long.class)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Erasure predictions")
    class ErasurePredictions {

        @Test
        @DisplayName("new ArrayList<String>().getClass() == new ArrayList<Integer>().getClass()")
        void sameClassForEveryTypeArgument() {
            Predict.check(ErasureAnswers.SAME_CLASS_FOR_EVERY_TYPE_ARGUMENT,
                    ErasurePuzzles.sameClassForEveryTypeArgument());
        }

        @Test
        @DisplayName("adding 42 to a List<String> through a raw List, then size()")
        void addThroughRawType() {
            Predict.check(ErasureAnswers.ADD_THROUGH_RAW_TYPE, ErasurePuzzles.addThroughRawType());
        }

        @Test
        @DisplayName("reading element 0 of that List<String> into a String")
        void readFromPollutedList() {
            Predict.check(ErasureAnswers.READ_FROM_POLLUTED_LIST, ErasurePuzzles.readFromPollutedList());
        }

        @Test
        @DisplayName("storing 42 in an Object[] that is really a String[]")
        void storeInCovariantArray() {
            Predict.check(ErasureAnswers.STORE_IN_COVARIANT_ARRAY, ErasurePuzzles.storeInCovariantArray());
        }

        @Test
        @DisplayName("int.class.isInstance(42)")
        void primitiveClassIsInstance() {
            Predict.check(ErasureAnswers.PRIMITIVE_CLASS_IS_INSTANCE, ErasurePuzzles.primitiveClassIsInstance());
        }

        @Test
        @DisplayName("whether the generic superclass of new ArrayList<String>() { } mentions String")
        void subclassKeepsTypeArgument() {
            Predict.check(ErasureAnswers.SUBCLASS_KEEPS_TYPE_ARGUMENT, ErasurePuzzles.subclassKeepsTypeArgument());
        }
    }
}
