package practice.j05.ex06;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J05-06 · Heap and priority queue")
class TopWordsTest {

    private static List<String> words(String text) {
        return List.of(text.split(" "));
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("returns the most frequent words, most frequent first")
        void mostFrequentFirst() {
            var words = words("the cat and the dog and the bird");

            assertThat(TopWords.topK(words, 2)).containsExactly("the", "and");
        }

        @Test
        @DisplayName("puts words with the same count in alphabetical order")
        void tiesAlphabetical() {
            var words = words("pear apple pear apple fig");

            assertThat(TopWords.topK(words, 3)).containsExactly("apple", "pear", "fig");
        }

        @Test
        @DisplayName("returns only k words")
        void onlyK() {
            var words = words("the cat and the dog and the bird");

            assertThat(TopWords.topK(words, 1)).containsExactly("the");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("returns every word when k is larger than the number of different words")
        void kLargerThanDistinct() {
            assertThat(TopWords.topK(words("b a b"), 10)).containsExactly("b", "a");
        }

        @Test
        @DisplayName("returns every word when k equals the number of different words")
        void kEqualsDistinct() {
            assertThat(TopWords.topK(words("b a b"), 2)).containsExactly("b", "a");
        }

        @Test
        @DisplayName("breaks a tie for the last place alphabetically")
        void tieForLastPlace() {
            var words = words("kiwi fig date lime fig");

            assertThat(TopWords.topK(words, 2)).containsExactly("fig", "date");
        }

        @Test
        @DisplayName("an empty list gives an empty result")
        void emptyInput() {
            assertThat(TopWords.topK(List.of(), 3)).isEmpty();
        }

        @Test
        @DisplayName("a single word is its own top 1")
        void singleWord() {
            assertThat(TopWords.topK(List.of("solo"), 1)).containsExactly("solo");
        }

        @Test
        @DisplayName("treats words in different letter case as different words")
        void caseSensitive() {
            var words = words("Java java java");

            assertThat(TopWords.topK(words, 2)).containsExactly("java", "Java");
        }

        @Test
        @DisplayName("returns a list that can't be modified")
        void unmodifiableResult() {
            List<String> top = TopWords.topK(words("a b"), 2);

            assertThatThrownBy(() -> top.add("c")).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("handles 100,000 words, comparing words as text: \"w10\" comes before \"w2\"")
        void largeInput() {
            List<String> words = new ArrayList<>();
            for (int i = 0; i < 100_000; i++) {
                words.add("w" + (i % 1_000)); // 1,000 different words, 100 times each
            }
            for (int i = 0; i < 5; i++) {
                words.add("w999");
            }

            assertThat(TopWords.topK(words, 4)).containsExactly("w999", "w0", "w1", "w10");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "k = {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("rejects k below 1")
        void rejectsKBelowOne(int k) {
            assertThatThrownBy(() -> TopWords.topK(words("a b"), k)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a null list")
        void rejectsNullList() {
            assertThatThrownBy(() -> TopWords.topK(null, 1)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a null word")
        void rejectsNullWord() {
            List<String> words = Arrays.asList("a", null, "b");

            assertThatThrownBy(() -> TopWords.topK(words, 1)).isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        /** A slow but clearly correct model: count every word, sort all of them, keep the first k. */
        private static List<String> sortAllAndTakeK(List<String> words, int k) {
            Map<String, Integer> counts = new HashMap<>();
            for (String word : words) {
                counts.merge(word, 1, Integer::sum);
            }
            List<String> distinct = new ArrayList<>(counts.keySet());
            distinct.sort(Comparator.comparing((String word) -> counts.get(word)).reversed()
                    .thenComparing(Comparator.naturalOrder()));
            return distinct.subList(0, Math.min(k, distinct.size()));
        }

        @Test
        @DisplayName("matches counting and sorting every word, for 500 random inputs")
        void matchesSortingModel() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            List<String> vocabulary = List.of("ant", "bee", "cat", "dog", "eel", "fox", "gnu", "hen", "owl", "yak");

            for (int trial = 0; trial < 500; trial++) {
                List<String> words = new ArrayList<>();
                int length = random.nextInt(40);
                for (int i = 0; i < length; i++) {
                    words.add(vocabulary.get(random.nextInt(vocabulary.size())));
                }
                int k = 1 + random.nextInt(12);

                assertThat(TopWords.topK(words, k)).as("trial %d: topK(%s, %d)", trial, words, k)
                        .isEqualTo(sortAllAndTakeK(words, k));
            }
        }
    }
}
