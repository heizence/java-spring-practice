package practice.j04.ex01;

import java.util.List;
import java.util.Objects;

/**
 * Generic helper methods for lists.
 *
 * <p>None of the methods returns {@code null}, and none accepts a {@code null} list
 * ({@link NullPointerException}).
 */
public final class ListHelpers {

    private ListHelpers() {
    }

    /**
     * Returns the first element of {@code list}, or {@code fallback} if the list is empty.
     *
     * @throws NullPointerException if {@code list} or {@code fallback} is {@code null}, even when the list isn't
     *                              empty, or if the first element is {@code null}
     */
    public static <T> T firstOrDefault(List<T> list, T fallback) {
        Objects.requireNonNull(list, "list");
        // Check the fallback even when it isn't needed: a bug in the caller shows up on the first call, not
        // only on the rare day the list is empty.
        Objects.requireNonNull(fallback, "fallback");
        if (list.isEmpty()) {
            return fallback;
        }
        return Objects.requireNonNull(list.getFirst(), "the first element is null");
    }

    /**
     * Returns the last {@code n} elements of {@code list}, in their original order, as an unmodifiable copy:
     * {@code lastN(List.of(1, 2, 3, 4, 5), 2)} is {@code [4, 5]}.
     *
     * <ul>
     *   <li>If {@code n} is larger than the size of the list, the result holds the whole list.</li>
     *   <li>If {@code n} is 0, the result is empty.</li>
     *   <li>Later changes to {@code list} don't affect the result.</li>
     * </ul>
     *
     * @throws IllegalArgumentException if {@code n} is negative
     * @throws NullPointerException     if {@code list} is {@code null}, or one of the last {@code n} elements is
     *                                  {@code null}
     */
    public static <T> List<T> lastN(List<T> list, int n) {
        Objects.requireNonNull(list, "list");
        if (n < 0) {
            throw new IllegalArgumentException("n must not be negative, but was " + n);
        }
        int from = Math.max(0, list.size() - n);
        // subList is only a view of the original list: it changes when the list changes. List.copyOf turns it
        // into an independent, unmodifiable list.
        return List.copyOf(list.subList(from, list.size()));
    }
}
