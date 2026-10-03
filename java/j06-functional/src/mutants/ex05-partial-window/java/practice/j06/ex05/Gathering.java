package practice.j06.ex05;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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
        Objects.requireNonNull(items, "items");
        // windowFixed checks the size too, but our message names the parameter the caller knows.
        requireAtLeastOne(size, "size");
        return items.stream()
                .gather(Gatherers.windowFixed(size))
                .toList();
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
        Objects.requireNonNull(values, "values");
        requireAtLeastOne(window, "window");
        return values.stream()
                .gather(Gatherers.windowSliding(window))
                .map(run -> run.stream().mapToInt(Integer::intValue).average().orElseThrow())
                .toList();
    }

    /**
     * The account balance after each amount, starting from {@code opening}. Positive amounts are deposits, negative
     * ones are withdrawals. Use {@link Gatherers#scan}.
     *
     * <p>For example, an opening balance of 100 with the amounts {@code [50, -30]} gives {@code [150, 120]}. The
     * opening balance itself isn't part of the result, so no amounts give an empty list.
     */
    public static List<Long> runningBalances(long opening, List<Long> amounts) {
        // scan emits the new value after every element: a running total, like reduce that shows its work.
        return amounts.stream()
                .gather(Gatherers.scan(() -> opening, Long::sum))
                .toList();
    }

    /**
     * Turns decimal digits into a number: {@code [1, 2, 3]} gives 123. No digits give 0, and leading zeros don't
     * change the value. Use {@link Gatherers#fold}.
     *
     * @throws IllegalArgumentException if a digit is outside 0 to 9
     * @throws ArithmeticException      if the number is too big for a {@code long}
     */
    public static long digitsToNumber(List<Integer> digits) {
        // fold is a reduce that runs in order, one element after the other, and emits exactly one value at the end,
        // even for an empty stream. reduce(0L, (n, d) -> n * 10 + d) would also need a combiner for parallel
        // streams, and this accumulator isn't associative, so reduce isn't the right tool.
        return digits.stream()
                .map(Gathering::checkDigit)
                .gather(Gatherers.fold(() -> 0L,
                        (number, digit) -> Math.addExact(Math.multiplyExact(number, 10), digit)))
                .findFirst()
                .orElseThrow(); // never empty: fold always emits its result
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
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        return Gatherer.ofSequential(
                // The initializer runs once per stream, so every stream gets its own empty set. A set created
                // out here, outside the initializer, would be shared by every stream that uses this gatherer.
                HashSet<K>::new,
                // Greedy: this integrator never stops the stream by itself, which lets the stream skip some checks.
                Gatherer.Integrator.ofGreedy((seen, element, downstream) -> {
                    if (seen.add(keyExtractor.apply(element))) { // HashSet accepts a null key
                        // push() returns false when the downstream wants no more elements (for example, limit()
                        // has enough). Returning that tells the stream to stop.
                        return downstream.push(element);
                    }
                    return true; // a duplicate: drop it and ask for the next element
                }));
    }

    private static Integer checkDigit(Integer digit) {
        if (digit < 0 || digit > 9) {
            throw new IllegalArgumentException("a digit must be between 0 and 9, but was " + digit);
        }
        return digit;
    }

    private static void requireAtLeastOne(int value, String name) {
        if (value < 1) {
            throw new IllegalArgumentException(name + " must be at least 1, but was " + value);
        }
    }
}
