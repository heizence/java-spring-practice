package practice.j01.ex04;

import java.time.DayOfWeek;
import java.util.Objects;

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
        // Integer division drops the last two digits: 404 / 100 is 4. That turns five range checks into a switch.
        return switch (code / 100) {
            case 1 -> StatusClass.INFORMATIONAL;
            case 2 -> StatusClass.SUCCESS;
            case 3 -> StatusClass.REDIRECTION;
            case 4 -> StatusClass.CLIENT_ERROR;
            case 5 -> StatusClass.SERVER_ERROR;
            // A switch expression must give a result for every input. Everything else, including 0 to 99 and
            // negative codes (-150 / 100 is -1), is not a status code.
            default -> throw new IllegalArgumentException("not an HTTP status code: " + code);
        };
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
        // No default branch: the cases cover every DayOfWeek constant, and the compiler checks that. If someone
        // added a constant to the enum, this switch would stop compiling instead of quietly returning a wrong
        // value. A switch on null throws NullPointerException, which is the rule we want anyway.
        return switch (day) {
            case MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY -> "09:00-21:00";
            case SATURDAY -> "10:00-18:00";
            case SUNDAY -> "closed";
        };
    }

    /**
     * Returns the sum of the decimal digits of {@code number}, ignoring its sign: 1234 gives 10, -1234 gives 10,
     * and 0 gives 0. It works for every {@code int}, including {@link Integer#MIN_VALUE} and
     * {@link Integer#MAX_VALUE}.
     *
     * <p>Use a {@code while} loop.
     */
    public static int digitSum(int number) {
        int sum = 0;
        // We don't make the number positive first: Math.abs(Integer.MIN_VALUE) is still negative, because
        // +2147483648 doesn't fit in an int. Instead we make each digit positive: -1234 % 10 is -4.
        while (number != 0) {
            sum += Math.abs(number % 10);
            number /= 10;
        }
        return sum;
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
        if (start < 1) {
            throw new IllegalArgumentException("start must be at least 1, but was " + start);
        }
        // long, not int: from start = 113383 the sequence climbs above Integer.MAX_VALUE. With int, 3 * n + 1
        // would silently wrap around to a negative number, and the loop would never reach 1.
        long n = start;
        int steps = 0;
        while (n != 1) {
            n = (n % 2 == 0) ? n / 2 : 3 * n + 1;
            steps++;
        }
        return steps;
    }

    /**
     * Finds the first place where two neighboring cells in the same row hold the same value, and returns the
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
     *   <li>Only neighbors in the same row count: not the last cell of a row and the first of the next, and not
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
        Objects.requireNonNull(grid, "grid");
        int[] found = new int[0];
        if (grid.length == 0) {
            return found;
        }
        int width = grid[0].length;
        search:
        for (int row = 0; row < grid.length; row++) {
            int[] cells = grid[row];
            for (int column = 0; column + 1 < width; column++) {
                if (cells[column] == cells[column + 1]) {
                    found = new int[] {row, column};
                    break search; // leaves both loops. A plain break would only leave the inner one.
                }
            }
        }
        return found;
    }
}
