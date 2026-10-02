package practice.j05.ex01;

import practice.j05.ex01.Puzzles.BigO;
import practice.j05.ex01.Puzzles.Outcome;

/**
 * Your answer sheet for J05-01. Replace each {@code null} with what you predict the matching method in
 * {@link Puzzles} returns, then run {@code PuzzlesTest}.
 *
 * <p>The constant's type tells you what to write: {@code "[x, y]"} for a {@code String} (a list or set prints
 * its elements in square brackets, separated by a comma and a space), {@code 3} for an {@code Integer},
 * {@code Outcome.RETURNS_NORMALLY} for an {@code Outcome}, and {@code BigO.O_1} for a {@code BigO}.
 */
final class Answers {

    private Answers() {
    }

    // Unmodifiable and fixed-size lists

    // List.of returns an unmodifiable list: every method that would change it throws.
    static final Outcome ADD_TO_LIST_OF = Outcome.THROWS_UNSUPPORTED_OPERATION_EXCEPTION;
    // Arrays.asList is a fixed-size view of the array: set() works, and it writes straight into the array.
    static final String SET_THROUGH_ARRAYS_AS_LIST = "[a, z, c]";
    // A fixed-size list can't grow or shrink, so add() and remove() throw.
    static final Outcome ADD_TO_ARRAYS_AS_LIST = Outcome.THROWS_UNSUPPORTED_OPERATION_EXCEPTION;

    // Views and copies

    // unmodifiableList is a read-only view: you can't change the list through it, but it shows changes to the source.
    static final String UNMODIFIABLE_VIEW_AFTER_CHANGE = "[a, b]";
    // List.copyOf takes a snapshot: later changes to the source don't show up in the copy.
    static final String COPY_OF_AFTER_CHANGE = "[a]";

    // Sequenced collections (Java 21)

    // reversed() returns a view in reverse order, not a reversed copy, so it also shows the 4 added later.
    static final String REVERSED_AFTER_CHANGE = "[4, 3, 2, 1]";
    // getFirst() on an empty list throws. (TypeScript's array[0] gives undefined instead.)
    static final Outcome GET_FIRST_OF_EMPTY_LIST = Outcome.THROWS_NO_SUCH_ELEMENT_EXCEPTION;

    // Removing elements while iterating

    // The loop's hidden iterator sees that the list changed behind its back and throws on its next step.
    static final Outcome REMOVE_INSIDE_FOR_EACH = Outcome.THROWS_CONCURRENT_MODIFICATION_EXCEPTION;
    // After "c" is removed, hasNext() is false: the loop ends early, never looks at "d", and doesn't throw.
    static final String REMOVE_SECOND_TO_LAST_INSIDE_FOR_EACH = "[a, b, d]";

    // remove(int) or remove(Object)?

    // An int argument picks remove(int index): it removes the element at index 1, which is 2.
    static final String REMOVE_WITH_INT = "[1, 3]";
    // An Integer argument picks remove(Object o): it removes the element equal to 1.
    static final String REMOVE_WITH_INTEGER = "[2, 3]";

    // The order of a set

    // TreeSet sorts with String.compareTo, which compares character codes: every uppercase letter comes first.
    static final String TREE_SET_ORDER = "[Apple, apple, banana, cherry]";
    // LinkedHashSet keeps the order of first insertion. Adding "a" again changes nothing.
    static final String LINKED_HASH_SET_ORDER = "[c, a, b]";

    // Keys that aren't in a map

    // get() returns null for a missing key, and string concatenation turns null into "null".
    static final String GET_MISSING_KEY = "pears: null";
    // get("pear") is null, and null + 1 must unbox null to an int. Use stock.merge("pear", 1, Integer::sum).
    static final Outcome INCREMENT_MISSING_KEY = Outcome.THROWS_NULL_POINTER_EXCEPTION;
    // The stored 3 for "apple", plus the default 0 for "pear".
    static final Integer GET_OR_DEFAULT = 3;

    // Big-O

    // An ArrayList is an array: the position of element i is computed, not searched.
    static final BigO ARRAY_LIST_GET_BY_INDEX = BigO.O_1;
    // Usually one write. Now and then the array is full and gets copied, but on average it's O(1) ("amortized").
    static final BigO ARRAY_LIST_ADD_AT_END = BigO.O_1;
    // Every element moves one place to the right to make room at index 0.
    static final BigO ARRAY_LIST_ADD_AT_FRONT = BigO.O_N;
    // contains() compares the elements one by one with equals().
    static final BigO ARRAY_LIST_CONTAINS = BigO.O_N;
    // A linked list has no index: get() walks node by node from the nearer end, so up to n / 2 steps.
    static final BigO LINKED_LIST_GET_BY_INDEX = BigO.O_N;
    // Adding at an end only links one new node.
    static final BigO LINKED_LIST_ADD_FIRST = BigO.O_1;
    // hashCode() points straight to a bucket, which holds about one entry when the hashes are well spread.
    static final BigO HASH_MAP_GET = BigO.O_1;
    // A TreeMap is a balanced search tree: each comparison discards about half of the remaining keys.
    static final BigO TREE_MAP_GET = BigO.O_LOG_N;
    // The smallest element is always at the root of the heap, index 0 of its array.
    static final BigO PRIORITY_QUEUE_PEEK = BigO.O_1;
    // The last element moves to the root and sinks one level per step, and a heap has about log n levels.
    static final BigO PRIORITY_QUEUE_POLL = BigO.O_LOG_N;
    // A heap only orders parents before children, so a search has to check every element.
    static final BigO PRIORITY_QUEUE_CONTAINS = BigO.O_N;
    // Sorting by comparing needs about n log n comparisons. List.sort uses TimSort, which is also stable.
    static final BigO ARRAY_LIST_SORT = BigO.O_N_LOG_N;
}
