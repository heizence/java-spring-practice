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
    static final Outcome ADD_TO_LIST_OF = null;
    static final String SET_THROUGH_ARRAYS_AS_LIST = null;
    static final Outcome ADD_TO_ARRAYS_AS_LIST = null;

    // Views and copies
    static final String UNMODIFIABLE_VIEW_AFTER_CHANGE = null;
    static final String COPY_OF_AFTER_CHANGE = null;

    // Sequenced collections (Java 21)
    static final String REVERSED_AFTER_CHANGE = null;
    static final Outcome GET_FIRST_OF_EMPTY_LIST = null;

    // Removing elements while iterating
    static final Outcome REMOVE_INSIDE_FOR_EACH = null;
    static final String REMOVE_SECOND_TO_LAST_INSIDE_FOR_EACH = null;

    // remove(int) or remove(Object)?
    static final String REMOVE_WITH_INT = null;
    static final String REMOVE_WITH_INTEGER = null;

    // The order of a set
    static final String TREE_SET_ORDER = null;
    static final String LINKED_HASH_SET_ORDER = null;

    // Keys that aren't in a map
    static final String GET_MISSING_KEY = null;
    static final Outcome INCREMENT_MISSING_KEY = null;
    static final Integer GET_OR_DEFAULT = null;

    // Big-O (the questions are at the bottom of Puzzles)
    static final BigO ARRAY_LIST_GET_BY_INDEX = null;
    static final BigO ARRAY_LIST_ADD_AT_END = null;
    static final BigO ARRAY_LIST_ADD_AT_FRONT = null;
    static final BigO ARRAY_LIST_CONTAINS = null;
    static final BigO LINKED_LIST_GET_BY_INDEX = null;
    static final BigO LINKED_LIST_ADD_FIRST = null;
    static final BigO HASH_MAP_GET = null;
    static final BigO TREE_MAP_GET = null;
    static final BigO PRIORITY_QUEUE_PEEK = null;
    static final BigO PRIORITY_QUEUE_POLL = null;
    static final BigO PRIORITY_QUEUE_CONTAINS = null;
    static final BigO ARRAY_LIST_SORT = null;
}
