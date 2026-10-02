package practice.j06.ex01;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

/**
 * Small higher-order functions: methods that take functions as arguments or return new functions.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Every method rejects {@code null} arguments with {@link NullPointerException} <em>right away</em>, not
 *       later when the returned function runs. A bug should show up where it was made.</li>
 *   <li>The functions this class returns are not thread-safe: use each one from one thread at a time.</li>
 *   <li>The last four methods practice the four kinds of method reference. Write each body as a single method
 *       reference of the kind its Javadoc names.</li>
 * </ul>
 */
public final class Functions {

    private Functions() {
    }

    public static <A, B, C> Function<A, C> compose(Function<? super A, ? extends B> first,
                                                   Function<? super B, ? extends C> second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        // A lambda instead of first.andThen(second): with these wildcard parameter types, andThen() would return a
        // Function<capture of ? super A, C>, which the compiler won't accept as a Function<A, C>.
        return input -> second.apply(first.apply(input));
    }

    public static <T, R> Function<T, R> memoize(Function<? super T, ? extends R> function) {
        Objects.requireNonNull(function, "function");
        // Each call of memoize() creates a new map, and the returned lambda captures it. That's how every memoized
        // function gets its own memory. The variable `cache` is effectively final; only the map's contents change.
        Map<T, R> cache = new HashMap<>();
        // computeIfAbsent() treats a null result as "nothing to store", so it would quietly call the function again
        // next time. requireNonNull turns that into a loud failure, and the exception means nothing gets stored.
        return input -> cache.computeIfAbsent(input,
                key -> Objects.requireNonNull(function.apply(key), () -> "function returned null for " + key));
    }

    public static <T> T retry(Supplier<? extends T> action, int attempts) {
        Objects.requireNonNull(action, "action");
        if (attempts < 1) {
            throw new IllegalArgumentException("attempts must be at least 1, but was " + attempts);
        }
        for (int attempt = 1; ; attempt++) {
            try {
                return action.get();
            } catch (RuntimeException e) {
                // RuntimeException, not Exception or Throwable: an Error means the JVM itself is in trouble, and
                // trying again won't help.
                if (attempt > attempts) {
                    throw e;
                }
            }
        }
    }

    public static IntSupplier counter(int start) {
        // `int next = start; return () -> next++;` doesn't compile: a lambda can't change a local variable.
        // So the count lives in a mutable object instead. AtomicInteger is the usual holder, and it wraps around
        // after Integer.MAX_VALUE like int arithmetic does.
        AtomicInteger next = new AtomicInteger(start);
        return next::getAndIncrement;
    }

    public static ToIntFunction<String> parser() {
        return Integer::parseInt;
    }

    public static Predicate<String> isIn(Collection<String> allowed) {
        // The receiver of a bound method reference is evaluated once, right here: Set.copyOf makes the copy now,
        // and a null collection fails now instead of on the first test().
        return Set.copyOf(allowed)::contains;
    }

    public static BiPredicate<String, String> startsWith() {
        // Unbound: the first argument becomes the object the method runs on, so this is (text, prefix) ->
        // text.startsWith(prefix).
        return String::startsWith;
    }

    public static Supplier<List<String>> listFactory() {
        return ArrayList::new;
    }
}
