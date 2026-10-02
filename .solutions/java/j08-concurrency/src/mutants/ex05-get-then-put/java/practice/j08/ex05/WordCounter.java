package practice.j08.ex05;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * Counts how often each word appears in many documents, on several threads.
 *
 * <h2>Rules for {@link #countParallel}</h2>
 * <ul>
 *   <li>Words are found by {@link #words(String)}: runs of letters or digits, in lowercase. So "Java" and "java"
 *       are the same word.</li>
 *   <li>Each document is counted by one task on a pool of {@code threads} threads. All tasks add their counts to one
 *       shared {@link java.util.concurrent.ConcurrentHashMap ConcurrentHashMap}, with an atomic update such as
 *       {@code merge}.</li>
 *   <li>The result maps each word to its total count over all documents. It can't be modified.</li>
 *   <li>No documents, or only documents without words, give an empty map.</li>
 *   <li>{@code threads} below 1 throws {@link IllegalArgumentException}. A {@code null} list or document throws
 *       {@link NullPointerException}.</li>
 * </ul>
 */
public final class WordCounter {

    private static final Pattern NOT_A_WORD_CHARACTER = Pattern.compile("[^\\p{L}\\p{N}]+");

    private WordCounter() {
    }

    /** Counts the words of all documents on {@code threads} threads. */
    public static Map<String, Long> countParallel(List<String> documents, int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1, but was " + threads);
        }
        // List.copyOf throws NullPointerException for a null list or a null document, before any task starts.
        List<String> checked = List.copyOf(documents);

        var counts = new ConcurrentHashMap<String, Long>();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            for (String document : checked) {
                // execute() instead of submit(): we don't need a result. If a task threw, the exception would be
                // printed by the thread's uncaught-exception handler instead of hiding in a Future nobody reads.
                pool.execute(() -> {
                    for (String word : words(document)) {
                        counts.put(word, counts.getOrDefault(word, 0L) + 1);
                    }
                });
            }
        } // close() waits for every task
        return Map.copyOf(counts);
    }

    /**
     * Splits a document into lowercase words. <strong>This method is finished</strong>; use it as it is.
     *
     * <p>Example: {@code words("Java, java & JAVA 25!")} returns {@code [java, java, java, 25]}.
     */
    static List<String> words(String document) {
        return Arrays.stream(NOT_A_WORD_CHARACTER.split(document.toLowerCase(Locale.ROOT)))
                .filter(word -> !word.isEmpty())
                .toList();
    }
}
