package practice.j06.ex05;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;

/**
 * Stream gatherers (Java 24+): intermediate operations that you can plug into a stream with
 * {@link java.util.stream.Stream#gather}. The first four methods use the built-in gatherers in {@link Gatherers}.
 * {@link #distinctBy} is a gatherer you write yourself.
 *
 * <p>Every method rejects {@code null} arguments with {@link NullPointerException}.
 */
public final class Gathering {

    private Gathering() {
    }

    /**
     * Splits {@code items} into batches of {@code size} items, in order. The last batch holds the items that are
     * left, so it can be smaller. Use {@link Gatherers#windowFixed}.
     *
     * <p>For example, {@code [a, b, c, d, e]} in batches of 2 gives {@code [[a, b], [c, d], [e]]}. No items give no
     * batches. The batches are unmodifiable.
     *
     * @throws IllegalArgumentException if {@code size} is less than 1
     */
    public static <T> List<List<T>> batches(List<T> items, int size) {
        // TODO: J06-05
        throw new UnsupportedOperationException("TODO: J06-05");
    }

    /**
     * The average of every run of {@code window} consecutive values, such as a moving average of response times.
     * Use {@link Gatherers#windowSliding}.
     *
     * <p>For example, {@code [1, 2, 3, 4]} with a window of 2 gives {@code [1.5, 2.5, 3.5]}. With fewer values than
     * {@code window}, there is no full window, so the result is empty.
     *
     * @throws IllegalArgumentException if {@code window} is less than 1
     */
    public static List<Double> movingAverages(List<Integer> values, int window) {
        // TODO: J06-05
        throw new UnsupportedOperationException("TODO: J06-05");
    }

    /**
     * The account balance after each amount, starting from {@code opening}. Positive amounts are deposits, negative
     * ones are withdrawals. Use {@link Gatherers#scan}.
     *
     * <p>For example, an opening balance of 100 with the amounts {@code [50, -30]} gives {@code [150, 120]}. The
     * opening balance itself isn't part of the result, so no amounts give an empty list.
     */
    public static List<Long> runningBalances(long opening, List<Long> amounts) {
        // TODO: J06-05
        throw new UnsupportedOperationException("TODO: J06-05");
    }

    /**
     * Turns decimal digits into a number: {@code [1, 2, 3]} gives 123. No digits give 0, and leading zeros don't
     * change the value. Use {@link Gatherers#fold}.
     *
     * @throws IllegalArgumentException if a digit is outside 0 to 9
     * @throws ArithmeticException      if the number is too big for a {@code long}
     */
    public static long digitsToNumber(List<Integer> digits) {
        // TODO: J06-05
        throw new UnsupportedOperationException("TODO: J06-05");
    }

    /**
     * A gatherer that keeps the first element for each key and drops later elements with the same key. The kept
     * elements stay in encounter order. Keys are compared with {@code equals}, and {@code null} is a key like any
     * other.
     *
     * <p>For example, {@code ["apple", "avocado", "banana"]} gathered with {@code distinctBy(w -> w.charAt(0))}
     * gives {@code ["apple", "banana"]}.
     *
     * <p>The same gatherer can be used for several streams: each stream starts with nothing seen. It works with
     * short-circuiting operations such as {@code limit}.
     *
     * @throws NullPointerException right away if {@code keyExtractor} is {@code null}
     */
    public static <T, K> Gatherer<T, ?, T> distinctBy(Function<? super T, ? extends K> keyExtractor) {
        // TODO: J06-05
        throw new UnsupportedOperationException("TODO: J06-05");
    }
}
