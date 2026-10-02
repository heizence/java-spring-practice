package practice.j04.ex02;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * Four small generic utilities in the style of {@link java.util.Collections}.
 *
 * <h2>Your task has two parts</h2>
 * <ol>
 *   <li><strong>Behavior.</strong> Implement the methods. The "Basic", "Edge cases" and "Errors" tests check
 *       them.</li>
 *   <li><strong>Signatures.</strong> The signatures in the exercise compile and work, but they are too strict:
 *       each method accepts fewer types than it safely could. Make them as flexible as possible with wildcards
 *       ({@code ? extends}, {@code ? super}) and bounds. The "Signatures" tests check them. Change only type
 *       arguments and the bound of {@code T}. Keep the method names, the name {@code T}, the order of the
 *       parameters, the container types ({@code Collection}, {@code List}, {@code Comparator},
 *       {@code Predicate}) and the return types.</li>
 * </ol>
 *
 * <p>Every method throws {@link NullPointerException} for a {@code null} argument.
 */
public final class CollectionTools {

    private CollectionTools() {
    }

    /**
     * Returns the largest element of {@code items} according to {@code comparator}. If several elements are
     * equally large, it returns the first of them, in the order of iteration.
     *
     * <p><strong>Signature:</strong> {@code items} produces the elements that {@code max} reads, and
     * {@code comparator} consumes them. With the exercise's signature, a call with a {@code List<Integer>} and a
     * {@code Comparator<Number>} doesn't compile.
     *
     * @throws NoSuchElementException if {@code items} is empty
     */
    public static <T> T max(Collection<? extends T> items, Comparator<? super T> comparator) {
        Objects.requireNonNull(items, "items");
        Objects.requireNonNull(comparator, "comparator");
        if (items.isEmpty()) {
            throw new NoSuchElementException("items is empty");
        }
        T best = items.iterator().next();
        for (T item : items) {           // reading from a Collection<? extends T> gives a T
            // Only a larger element replaces the best one, so among equal elements the first one wins.
            if (comparator.compare(item, best) > 0) {   // a Comparator<? super T> accepts any T
                best = item;
            }
        }
        return best;
    }

    /**
     * Adds every element of {@code source} that matches {@code condition} to the end of {@code target}, in the
     * order of {@code source}, and returns how many elements it added. The elements already in {@code target}
     * stay where they are.
     *
     * <p><strong>Signature:</strong> {@code source} produces elements, and {@code condition} and {@code target}
     * consume them. With the exercise's signature, copying from a {@code List<Integer>} into a
     * {@code List<Number>} with a {@code Predicate<Number>} doesn't compile.
     */
    public static <T> int copyIf(List<? extends T> source, Predicate<? super T> condition, List<? super T> target) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(condition, "condition");
        Objects.requireNonNull(target, "target");
        int added = 0;
        for (T item : source) {             // get from a producer: ? extends T
            if (condition.test(item)) {     // give to a consumer: ? super T
                target.add(item);           // give to a consumer: ? super T
                added++;
            }
        }
        return added;
    }

    /**
     * Returns the sum of {@code numbers} as a {@code double}, using {@link Number#doubleValue()}. An empty
     * collection sums to {@code 0.0}.
     *
     * <p><strong>Signature:</strong> with the exercise's signature, only a {@code Collection<Number>} is
     * accepted. It should accept a collection of any kind of number: a {@code List<Integer>}, a
     * {@code Set<Long>}, a {@code List<BigDecimal>}.
     *
     * @throws NullPointerException if an element is {@code null}
     */
    public static double sum(Collection<Number> numbers) {
        Objects.requireNonNull(numbers, "numbers");
        double total = 0;
        for (Number number : numbers) {
            total += number.doubleValue();
        }
        return total;
    }

    /**
     * Returns {@code value} limited to the range from {@code min} to {@code max}, both included: {@code min} if
     * {@code value} is smaller, {@code max} if it is larger, and {@code value} itself otherwise. It compares with
     * {@code compareTo}.
     *
     * <p><strong>Signature:</strong> with the exercise's signature, {@code T} must be comparable to exactly
     * {@code T}. But {@code LocalDate} implements {@code Comparable<ChronoLocalDate>}, where
     * {@code ChronoLocalDate} is an interface that {@code LocalDate} implements, so
     * {@code LocalDate d = clamp(date, start, end)} doesn't compile. Loosen the bound of {@code T}.
     *
     * @throws IllegalArgumentException if {@code min} is greater than {@code max}. The message contains both.
     */
    public static <T extends Comparable<? super T>> T clamp(T value, T min, T max) {
        Objects.requireNonNull(value, "value");
        Objects.requireNonNull(min, "min");
        Objects.requireNonNull(max, "max");
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("min " + min + " is greater than max " + max);
        }
        if (value.compareTo(min) < 0) {
            return min;
        }
        if (value.compareTo(max) > 0) {
            return max;
        }
        return value;
    }
}
