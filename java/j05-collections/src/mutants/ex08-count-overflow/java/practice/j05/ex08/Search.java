package practice.j05.ex08;

import java.util.Objects;

/**
 * Binary search on a sorted {@code int} array, the way database indexes and {@code TreeMap} find a range of
 * keys.
 *
 * <h2>Rules for every method</h2>
 * <ul>
 *   <li>{@code sorted} must be sorted from smallest to largest. It may contain duplicates. If it isn't sorted,
 *       the result is meaningless.</li>
 *   <li>Each method runs in O(log n): no loop over all elements.</li>
 *   <li>A {@code null} array throws {@link NullPointerException}. An empty array is fine.</li>
 * </ul>
 */
public final class Search {

    private Search() {
    }

    /**
     * The first index whose value is greater than or equal to {@code target}, or {@code sorted.length} if
     * there is none. It's where {@code target} would be inserted to keep the array sorted, before any equal
     * values.
     */
    public static int lowerBound(int[] sorted, int target) {
        Objects.requireNonNull(sorted, "sorted");
        // The answer is always in [low, high]. Everything before low is < target; everything from high on
        // is >= target. Each step halves the range, until low == high.
        int low = 0;
        int high = sorted.length;
        while (low < high) {
            // Not (low + high) / 2: for arrays longer than about a billion elements, low + high overflows.
            int middle = low + (high - low) / 2;
            if (sorted[middle] < target) {
                low = middle + 1; // middle and everything before it are too small
            } else {
                high = middle; // middle may be the answer, so keep it in the range
            }
        }
        return low;
    }

    /**
     * The first index whose value is greater than {@code target}, or {@code sorted.length} if there is none.
     * It's where {@code target} would be inserted to keep the array sorted, after any equal values.
     */
    public static int upperBound(int[] sorted, int target) {
        Objects.requireNonNull(sorted, "sorted");
        int low = 0;
        int high = sorted.length;
        while (low < high) {
            int middle = low + (high - low) / 2;
            if (sorted[middle] <= target) { // the only difference from lowerBound: <= instead of <
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    /** How many values lie between {@code low} and {@code high}, both included; 0 if {@code low > high}. */
    public static int countInRange(int[] sorted, int low, int high) {
        Objects.requireNonNull(sorted, "sorted");
        if (low > high) {
            return 0;
        }
        // The values in the range sit between these two indexes.
        return lowerBound(sorted, high + 1) - lowerBound(sorted, low);
    }
}
