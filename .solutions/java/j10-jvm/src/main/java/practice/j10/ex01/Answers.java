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

    // Parameters are local variables of the method: they live in its stack frame and vanish when it returns.
    static final Where PARAMETER_QUANTITY = Where.STACK;
    // A local variable of a primitive type holds the number itself, right in the frame.
    static final Where LOCAL_UNIT_PRICE = Where.STACK;
    // The variable is local, so it is in the frame. It holds only a reference to the object.
    static final Where ORDER_VARIABLE = Where.STACK;
    // Every object created with new lives in the heap, shared by all threads.
    static final Where ORDER_OBJECT = Where.HEAP;
    // A field is part of its object, so a primitive field lives in the heap too. Only local variables use the stack.
    static final Where ORDER_ID_FIELD = Where.HEAP;
    // An array is an object. An int[] stores the ints themselves inside that object.
    static final Where ARRAY_ELEMENTS = Where.HEAP;
    // A static field belongs to the class, not to a call. HotSpot keeps it in the Class object, in the heap.
    static final Where STATIC_COUNTER = Where.HEAP;

    // 2. Garbage collection: is the object eligible at "<- here"?

    // After the assignment, no variable or object refers to Order 1 any more.
    static final Boolean REASSIGNED = true;
    // sameOrder still refers to it. Setting one reference to null doesn't free an object.
    static final Boolean SECOND_REFERENCE = false;
    // The GC looks for objects that running code can reach; it doesn't count references.
    // So a cycle that nothing reaches is garbage.
    static final Boolean CYCLE = true;
    // A static field keeps everything it refers to reachable, as long as the class is loaded: the classic leak.
    static final Boolean STATIC_LIST = false;
    // An inner-class object holds a hidden reference to its outer object (Report.this), and line is still in use.
    static final Boolean INNER_OBJECT = false;

    // 3. How does the program end?

    // The base case exists, but a million nested calls need a million stack frames; 512 KB holds a few thousand.
    static final Ending DEEP_RECURSION = Ending.STACK_OVERFLOW_ERROR;
    // book.toString() prints the author, whose toString() prints its books, whose toString() prints the author...
    static final Ending CYCLIC_TO_STRING = Ending.STACK_OVERFLOW_ERROR;
    // 1,000 MB in total, but only one report is reachable at a time; the GC reclaims the others.
    static final Ending GARBAGE_IN_A_LOOP = Ending.COMPLETES;
    // The same reports, but the static list keeps all of them reachable: the heap fills up after about 15.
    static final Ending GROWING_CACHE = Ending.OUT_OF_MEMORY_ERROR;
    // 1,000,000 ints of 4 bytes each: one 4 MB array.
    static final Ending PRIMITIVE_ARRAY = Ending.COMPLETES;
    // Each Integer is an object of 16 bytes plus a 4-byte reference in the list: over 20 MB for the same numbers.
    static final Ending BOXED_LIST = Ending.OUT_OF_MEMORY_ERROR;

    // 4. Reading stack traces

    // The top frame is where the exception was thrown.
    static final String NPE_THROWN_AT = "CouponService.java:31";
    // code is a parameter, so the null came from the caller: the next frame down.
    static final String NPE_NULL_CAME_FROM = "CheckoutService.java:57";
    // Skip the JDK frames (java.base/...): the first frame in com.example.shop called add() on an unmodifiable list.
    static final String FIRST_LINE_OF_YOUR_CODE = "GiftService.java:22";
    // The last "Caused by" is the original problem; its first frame in your code is where Long.parseLong failed.
    static final String ROOT_CAUSE_IN_YOUR_CODE = "CsvOrderParser.java:61";
}
