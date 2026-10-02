package practice.j05.ex06;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.PriorityQueue;

/** Finds the most frequent words, for example the top search terms of the day. */
public final class TopWords {

    private TopWords() {
    }

    // A word with the number of times it appears.
    private record WordCount(String word, int count) {
    }

    // Better = more frequent; for the same count, alphabetically first.
    private static final Comparator<WordCount> BETTER_FIRST =
            Comparator.comparingInt(WordCount::count).reversed().thenComparing(WordCount::word);

    /**
     * The {@code k} most frequent words, most frequent first.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>Words with the same count are in alphabetical order ({@code String.compareTo}). This also decides
     *       which words make the cut when several words tie for the last place.</li>
     *   <li>Words are compared exactly as they are: {@code "Java"} and {@code "java"} are different words.</li>
     *   <li>If there are fewer than {@code k} different words, the result contains all of them.</li>
     *   <li>The result is an unmodifiable list. An empty input gives an empty list.</li>
     *   <li>{@code k} below 1 throws {@link IllegalArgumentException}. A {@code null} list or word throws
     *       {@link NullPointerException}.</li>
     *   <li>Use a {@link java.util.PriorityQueue}.</li>
     * </ul>
     */
    public static List<String> topK(List<String> words, int k) {
        Objects.requireNonNull(words, "words");
        if (k < 1) {
            throw new IllegalArgumentException("k must be at least 1, but was " + k);
        }
        Map<String, Integer> counts = new HashMap<>();
        for (String word : words) {
            Objects.requireNonNull(word, "words must not contain null");
            counts.merge(word, 1, Integer::sum); // insert 1, or add 1 to the existing count
        }

        // Keep only the k best words seen so far, in a heap ordered worst first. When it grows past k,
        // poll() drops the worst one. That costs O(n log k) time and O(k) memory, instead of sorting every
        // different word: it scales to "the top 10 of millions".
        PriorityQueue<WordCount> best = new PriorityQueue<>(BETTER_FIRST.reversed());
        counts.forEach((word, count) -> {
            best.offer(new WordCount(word, count));
            if (best.size() > k) {
                best.poll();
            }
        });

        // The heap hands out the worst word first, so the list comes out in reverse order.
        List<String> result = new ArrayList<>(best.size());
        while (!best.isEmpty()) {
            result.add(best.poll().word());
        }
        return List.copyOf(result.reversed());
    }
}
