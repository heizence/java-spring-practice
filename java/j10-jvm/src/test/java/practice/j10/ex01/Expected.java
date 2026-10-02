package practice.j10.ex01;

import practice.j10.ex01.Puzzles.Where;

/**
 * SPOILERS. The expected answers for the puzzles that no code can measure: where a value lives, whether an object
 * is reachable, and where to look in a stack trace. The "How does the program end?" puzzles don't need this file,
 * because their tests run the programs.
 *
 * <p>Make your predictions in {@code Answers} before you open this file.
 */
final class Expected {

    private Expected() {
    }

    // 1. Stack or heap?
    static final Where PARAMETER_QUANTITY = Where.STACK;
    static final Where LOCAL_UNIT_PRICE = Where.STACK;
    static final Where ORDER_VARIABLE = Where.STACK;
    static final Where ORDER_OBJECT = Where.HEAP;
    static final Where ORDER_ID_FIELD = Where.HEAP;
    static final Where ARRAY_ELEMENTS = Where.HEAP;
    static final Where STATIC_COUNTER = Where.HEAP;

    // 2. Garbage collection: is the object eligible at "<- here"?
    static final Boolean REASSIGNED = true;
    static final Boolean SECOND_REFERENCE = false;
    static final Boolean CYCLE = true;
    static final Boolean STATIC_LIST = false;
    static final Boolean INNER_OBJECT = false;

    // 4. Reading stack traces
    static final String NPE_THROWN_AT = "CouponService.java:31";
    static final String NPE_NULL_CAME_FROM = "CheckoutService.java:57";
    static final String FIRST_LINE_OF_YOUR_CODE = "GiftService.java:22";
    static final String ROOT_CAUSE_IN_YOUR_CODE = "CsvOrderParser.java:61";
}
