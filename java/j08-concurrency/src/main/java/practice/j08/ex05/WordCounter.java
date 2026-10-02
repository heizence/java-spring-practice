package practice.j08.ex05;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
        // TODO: J08-05
        throw new UnsupportedOperationException("TODO: J08-05");
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
