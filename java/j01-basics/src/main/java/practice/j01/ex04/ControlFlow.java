package practice.j01.ex04;

import java.time.DayOfWeek;

/** Small methods that practice Java's control flow: switch expressions, loops, and a labeled break. */
public final class ControlFlow {

    private ControlFlow() {
    }

    /**
     * Returns the class of an HTTP status code: 100 to 199 is {@link StatusClass#INFORMATIONAL}, 200 to 299
     * {@link StatusClass#SUCCESS}, 300 to 399 {@link StatusClass#REDIRECTION}, 400 to 499
     * {@link StatusClass#CLIENT_ERROR}, and 500 to 599 {@link StatusClass#SERVER_ERROR}.
     *
     * <p>Use a {@code switch} expression.
     *
     * @throws IllegalArgumentException for any other code. The message contains the code.
     */
    public static StatusClass statusClass(int code) {
        // TODO: J01-04
        throw new UnsupportedOperationException("TODO: J01-04");
    }

    /**
     * Returns the bookstore's opening hours on {@code day}: Monday to Friday {@code "09:00-21:00"}, Saturday
     * {@code "10:00-18:00"}, and Sunday {@code "closed"}.
     *
     * <p>Use a {@code switch} expression with several labels in one {@code case}.
     *
     * @throws NullPointerException if {@code day} is {@code null}
     */
    public static String openingHours(DayOfWeek day) {
        // TODO: J01-04
        throw new UnsupportedOperationException("TODO: J01-04");
    }

    /**
     * Returns the sum of the decimal digits of {@code number}, ignoring its sign: 1234 gives 10, -1234 gives 10,
     * and 0 gives 0. It works for every {@code int}, including {@link Integer#MIN_VALUE} and
     * {@link Integer#MAX_VALUE}.
     *
     * <p>Use a {@code while} loop.
     */
    public static int digitSum(int number) {
        // TODO: J01-04
        throw new UnsupportedOperationException("TODO: J01-04");
    }

    /**
     * Counts the steps of the Collatz sequence from {@code start} until it reaches 1. In each step, an even
     * {@code n} becomes {@code n / 2}, and an odd {@code n} becomes {@code 3 * n + 1}.
     *
     * <p>Examples: {@code collatzSteps(1)} is 0, and {@code collatzSteps(6)} is 8, because the sequence is
     * 6, 3, 10, 5, 16, 8, 4, 2, 1.
     *
     * <p>The numbers in a sequence can grow larger than {@link Integer#MAX_VALUE}, even when {@code start} is
     * small. The count must still be right.
     *
     * @throws IllegalArgumentException if {@code start} is less than 1
     */
    public static int collatzSteps(int start) {
        // TODO: J01-04
        throw new UnsupportedOperationException("TODO: J01-04");
    }

    /**
     * Finds the first place where two neighbouring cells in the same row hold the same value, and returns the
     * position {@code {row, column}} of the left cell of that pair.
     *
     * <pre>
     * { {1, 2, 3},
     *   {4, 5, 5},     the first pair is in row 1, columns 1 and 2
     *   {7, 7, 8} }    so the result is {1, 1}
     * </pre>
     *
     * <ul>
     *   <li>The search goes row by row from the top, and from left to right within a row.</li>
     *   <li>Only neighbours in the same row count: not the last cell of a row and the first of the next, and not
     *       cells above each other.</li>
     *   <li>Rows may have different lengths, and may be empty.</li>
     *   <li>If there is no pair, the result is an empty array.</li>
     * </ul>
     *
     * <p>Practice: leave both loops at once with a labeled {@code break}.
     *
     * @throws NullPointerException if {@code grid} is {@code null}
     */
    public static int[] firstPair(int[][] grid) {
        // TODO: J01-04
        throw new UnsupportedOperationException("TODO: J01-04");
    }
}
