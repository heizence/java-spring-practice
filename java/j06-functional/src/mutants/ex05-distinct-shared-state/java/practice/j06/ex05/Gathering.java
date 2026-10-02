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

    public static <T> List<List<T>> batches(List<T> items, int size) {
        Objects.requireNonNull(items, "items");
        // windowFixed checks the size too, but our message names the parameter the caller knows.
        requireAtLeastOne(size, "size");
        return items.stream()
                .gather(Gatherers.windowFixed(size))
                .toList();
    }

    public static List<Double> movingAverages(List<Integer> values, int window) {
        Objects.requireNonNull(values, "values");
        requireAtLeastOne(window, "window");
        if (values.size() < window) {
            // windowSliding emits one short window when the stream is shorter than the window. An average over
            // fewer values than asked for would be misleading, so there is no result at all.
            return List.of();
        }
        return values.stream()
                .gather(Gatherers.windowSliding(window))
                .map(run -> run.stream().mapToInt(Integer::intValue).average().orElseThrow())
                .toList();
    }

    public static List<Long> runningBalances(long opening, List<Long> amounts) {
        // scan emits the new value after every element: a running total, like reduce that shows its work.
        return amounts.stream()
                .gather(Gatherers.scan(() -> opening, Long::sum))
                .toList();
    }

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

    public static <T, K> Gatherer<T, ?, T> distinctBy(Function<? super T, ? extends K> keyExtractor) {
        Objects.requireNonNull(keyExtractor, "keyExtractor");
        HashSet<K> seenKeys = new HashSet<>();
        return Gatherer.ofSequential(
                () -> seenKeys,
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
