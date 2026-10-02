package practice.j01.ex05;

/**
 * Helpers for {@code int} and {@code String} arrays.
 *
 * <ul>
 *   <li>A method never changes the array you pass in, unless its name ends with {@code InPlace}.</li>
 *   <li>Every method throws {@link NullPointerException} for a {@code null} array.</li>
 * </ul>
 */
public final class ArrayTools {

    private ArrayTools() {
    }

    /**
     * Returns the largest value in {@code values}.
     *
     * @throws IllegalArgumentException if {@code values} is empty: an empty array has no largest value
     */
    public static int max(int[] values) {
        // TODO: J01-05
        throw new UnsupportedOperationException("TODO: J01-05");
    }

    /**
     * Reverses the order of the elements of {@code values}, changing the array itself:
     * {@code {1, 2, 3}} becomes {@code {3, 2, 1}}.
     */
    public static void reverseInPlace(int[] values) {
        // TODO: J01-05
        throw new UnsupportedOperationException("TODO: J01-05");
    }

    /**
     * Returns a new array with the elements moved {@code k} positions to the right. Elements that move past the
     * end come back at the start: {@code rotateRight({1, 2, 3, 4, 5}, 2)} returns {@code {4, 5, 1, 2, 3}}.
     *
     * <ul>
     *   <li>{@code k} may be larger than the length: moving 5 elements by 7 positions is the same as moving them
     *       by 2.</li>
     *   <li>A negative {@code k} moves the elements to the left: {@code rotateRight({1, 2, 3, 4, 5}, -1)}
     *       returns {@code {2, 3, 4, 5, 1}}.</li>
     *   <li>Every {@code int} is a valid {@code k}, including {@link Integer#MIN_VALUE}.</li>
     *   <li>An empty array gives an empty array.</li>
     * </ul>
     */
    public static int[] rotateRight(int[] values, int k) {
        // TODO: J01-05
        throw new UnsupportedOperationException("TODO: J01-05");
    }

    /**
     * Returns the average of {@code values}: {@code average(1, 2)} is {@code 1.5}.
     *
     * <p>Large values must not overflow: {@code average(Integer.MAX_VALUE, Integer.MAX_VALUE)} is
     * {@code 2147483647.0}.
     *
     * @throws IllegalArgumentException if there are no values
     */
    public static double average(int... values) {
        // TODO: J01-05
        throw new UnsupportedOperationException("TODO: J01-05");
    }

    /** Returns the index of the first element that equals {@code target}, or -1 if there is none. */
    public static int indexOf(int[] values, int target) {
        // TODO: J01-05
        throw new UnsupportedOperationException("TODO: J01-05");
    }

    /**
     * Returns the index of the first element that equals {@code target}, or -1 if there is none.
     *
     * <ul>
     *   <li>Elements are compared with {@code equals}, not {@code ==}: a {@code String} with the same text counts,
     *       even when it is a different object.</li>
     *   <li>The array may contain {@code null} elements, and {@code target} may be {@code null}. A {@code null}
     *       target finds the first {@code null} element.</li>
     * </ul>
     */
    public static int indexOf(String[] values, String target) {
        // TODO: J01-05
        throw new UnsupportedOperationException("TODO: J01-05");
    }
}
