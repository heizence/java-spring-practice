package practice.j05.ex06;

import java.util.List;

/** Finds the most frequent words, for example the top search terms of the day. */
public final class TopWords {

    private TopWords() {
    }

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
        // TODO: J05-06
        throw new UnsupportedOperationException("TODO: J05-06");
    }
}
