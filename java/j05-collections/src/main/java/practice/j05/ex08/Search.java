package practice.j05.ex08;

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
        // TODO: J05-08
        throw new UnsupportedOperationException("TODO: J05-08");
    }

    /**
     * The first index whose value is greater than {@code target}, or {@code sorted.length} if there is none.
     * It's where {@code target} would be inserted to keep the array sorted, after any equal values.
     */
    public static int upperBound(int[] sorted, int target) {
        // TODO: J05-08
        throw new UnsupportedOperationException("TODO: J05-08");
    }

    /** How many values lie between {@code low} and {@code high}, both included; 0 if {@code low > high}. */
    public static int countInRange(int[] sorted, int low, int high) {
        // TODO: J05-08
        throw new UnsupportedOperationException("TODO: J05-08");
    }
}
