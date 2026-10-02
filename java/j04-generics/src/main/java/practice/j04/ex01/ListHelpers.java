package practice.j04.ex01;

import java.util.List;

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
        // TODO: J04-01
        throw new UnsupportedOperationException("TODO: J04-01");
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
        // TODO: J04-01
        throw new UnsupportedOperationException("TODO: J04-01");
    }
}
