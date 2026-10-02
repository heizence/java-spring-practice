package practice.j10.ex01;

import practice.j10.ex01.Puzzles.Ending;
import practice.j10.ex01.Puzzles.Where;

/**
 * Your answer sheet for J10-01. Replace each {@code null} with your prediction for the matching puzzle in
 * {@link Puzzles}, then run {@code PuzzlesTest}.
 *
 * <p>The type of each constant tells you what to write: {@code Where.STACK} or {@code Where.HEAP} for a
 * {@code Where}, {@code true} or {@code false} for a {@code Boolean}, {@code Ending.COMPLETES} (or another
 * {@code Ending}) for an {@code Ending}, and a location such as {@code "Main.java:12"} for a {@code String}.
 */
final class Answers {

    private Answers() {
    }

    // 1. Stack or heap?
    static final Where PARAMETER_QUANTITY = null;
    static final Where LOCAL_UNIT_PRICE = null;
    static final Where ORDER_VARIABLE = null;
    static final Where ORDER_OBJECT = null;
    static final Where ORDER_ID_FIELD = null;
    static final Where ARRAY_ELEMENTS = null;
    static final Where STATIC_COUNTER = null;

    // 2. Garbage collection: is the object eligible at "<- here"?
    static final Boolean REASSIGNED = null;
    static final Boolean SECOND_REFERENCE = null;
    static final Boolean CYCLE = null;
    static final Boolean STATIC_LIST = null;
    static final Boolean INNER_OBJECT = null;

    // 3. How does the program end?
    static final Ending DEEP_RECURSION = null;
    static final Ending CYCLIC_TO_STRING = null;
    static final Ending GARBAGE_IN_A_LOOP = null;
    static final Ending GROWING_CACHE = null;
    static final Ending PRIMITIVE_ARRAY = null;
    static final Ending BOXED_LIST = null;

    // 4. Reading stack traces
    static final String NPE_THROWN_AT = null;
    static final String NPE_NULL_CAME_FROM = null;
    static final String FIRST_LINE_OF_YOUR_CODE = null;
    static final String ROOT_CAUSE_IN_YOUR_CODE = null;
}
