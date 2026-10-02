package practice.j10.ex01;

import java.util.ArrayList;
import java.util.List;

import practice.j10.ex01.programs.BoxedList;
import practice.j10.ex01.programs.CyclicToString;
import practice.j10.ex01.programs.DeepRecursion;
import practice.j10.ex01.programs.GarbageInALoop;
import practice.j10.ex01.programs.GrowingCache;
import practice.j10.ex01.programs.PrimitiveArray;

/**
 * J10-01 · Predict: stack, heap and errors.
 *
 * <p>Four groups of puzzles. <strong>Don't run anything yet.</strong> Read each puzzle, decide the answer, and write
 * it in {@link Answers}. Then run the tests: a test fails when a prediction was wrong, and the module README explains
 * every answer.
 */
final class Puzzles {

    private Puzzles() {
    }

    // ---- 1. Stack or heap? ---------------------------------------------------------------------------------
    //
    // Picture the moment when checkout() reaches the line marked "<- now". Where does each value live: in the
    // stack frame of checkout() (STACK), or in the heap (HEAP)? The comments name the constants in Answers.

    static long checkout(int quantity) {                   // PARAMETER_QUANTITY: the parameter quantity
        long unitPrice = 12_000;                            // LOCAL_UNIT_PRICE: the local variable unitPrice
        Order order = new Order(42, unitPrice * quantity);  // ORDER_VARIABLE: the variable order
                                                            // ORDER_OBJECT: the Order object it refers to
                                                            // ORDER_ID_FIELD: the field id inside that object
        int[] ratings = {5, 4, 3};                          // ARRAY_ELEMENTS: the numbers 5, 4 and 3
        Order.created++;                                    // STATIC_COUNTER: the static field Order.created
        return order.total() + ratings.length;              // <- now
    }

    static final class Order {
        static int created;      // a static field
        private final long id;   // instance fields
        private final long total;

        Order(long id, long total) {
            this.id = id;
            this.total = total;
        }

        long id() {
            return id;
        }

        long total() {
            return total;
        }
    }

    // ---- 2. Garbage collection -----------------------------------------------------------------------------
    //
    // An object is "eligible for garbage collection" when no running code can reach it any more, through any
    // chain of references. Is the object in question eligible at the line marked "<- here"? true or false.

    static void reassigned() {
        Order order = new Order(1, 10_000);   // the object in question: Order 1
        order = new Order(2, 20_000);
        // <- here
        System.out.println(order.id());
    }

    static void secondReference() {
        Order order = new Order(1, 10_000);   // the object in question: Order 1
        Order sameOrder = order;
        order = null;
        // <- here
        System.out.println(sameOrder.id());
    }

    static void cycle() {
        Node first = new Node("first");       // the objects in question: both nodes
        Node second = new Node("second");
        first.next = second;
        second.next = first;                  // the nodes refer to each other
        first = null;
        second = null;
        // <- here
    }

    private static final List<Order> RECENT_ORDERS = new ArrayList<>();

    static void staticList() {
        Order order = new Order(1, 10_000);   // the object in question: Order 1
        RECENT_ORDERS.add(order);
        order = null;
        // <- here
    }

    static void innerObject() {
        Report report = new Report("October sales");          // the object in question: the Report
        Report.Line line = report.new Line("books", 1_250_000);
        report = null;
        // <- here
        System.out.println(line.describe());
    }

    static final class Node {
        final String name;
        Node next;

        Node(String name) {
            this.name = name;
        }
    }

    static final class Report {
        private final String title;

        Report(String title) {
            this.title = title;
        }

        final class Line { // an inner class: not static
            private final String label;
            private final long amount;

            Line(String label, long amount) {
                this.label = label;
                this.amount = amount;
            }

            String describe() {
                return title + ": " + label + " " + amount; // title is Report.this.title
            }
        }
    }

    // ---- 3. How does the program end? ----------------------------------------------------------------------
    //
    // Each puzzle starts one of the programs in practice.j10.ex01.programs in a new JVM, with the options shown,
    // and reports how it ended (see ChildJvm). -Xss sets the stack size of each thread, -Xmx the maximum heap size.
    // Read the program, then predict: COMPLETES, STACK_OVERFLOW_ERROR or OUT_OF_MEMORY_ERROR.

    static Ending deepRecursion() {
        return ChildJvm.run(DeepRecursion.class, "-Xss512k");
    }

    static Ending cyclicToString() {
        return ChildJvm.run(CyclicToString.class, "-Xss512k");
    }

    static Ending garbageInALoop() {
        return ChildJvm.run(GarbageInALoop.class, "-Xmx16m");
    }

    static Ending growingCache() {
        return ChildJvm.run(GrowingCache.class, "-Xmx16m");
    }

    static Ending primitiveArray() {
        return ChildJvm.run(PrimitiveArray.class, "-Xmx16m");
    }

    static Ending boxedList() {
        return ChildJvm.run(BoxedList.class, "-Xmx16m");
    }

    // ---- 4. Reading stack traces ---------------------------------------------------------------------------
    //
    // Three stack traces from a shop application whose own code is in the package com.example.shop. Answer with a
    // location written as "File.java:line", for example "Main.java:12".

    /**
     * NPE_THROWN_AT: on which line was the exception thrown? NPE_NULL_CAME_FROM: on which line was the null passed
     * in? Here is the method at the top of the trace:
     *
     * <pre>{@code
     * // CouponService.java
     * 30    String normalize(String code) {
     * 31        return code.trim().toUpperCase(Locale.ROOT);
     * 32    }
     * }</pre>
     */
    static final String NULL_POINTER_TRACE = """
            java.lang.NullPointerException: Cannot invoke "String.trim()" because "code" is null
                at com.example.shop.CouponService.normalize(CouponService.java:31)
                at com.example.shop.CheckoutService.applyCoupon(CheckoutService.java:57)
                at com.example.shop.CheckoutService.checkout(CheckoutService.java:40)
                at com.example.shop.Main.main(Main.java:12)
            """;

    /** FIRST_LINE_OF_YOUR_CODE: which line of your own code should you open first? */
    static final String UNSUPPORTED_OPERATION_TRACE = """
            java.lang.UnsupportedOperationException
                at java.base/java.util.ImmutableCollections.uoe(ImmutableCollections.java:159)
                at java.base/java.util.ImmutableCollections$AbstractImmutableCollection.add(ImmutableCollections.java:164)
                at com.example.shop.GiftService.addGiftWrap(GiftService.java:22)
                at com.example.shop.CheckoutService.checkout(CheckoutService.java:44)
                at com.example.shop.Main.main(Main.java:12)
            """;

    /** ROOT_CAUSE_IN_YOUR_CODE: on which line of your own code did the original problem happen? */
    static final String CAUSED_BY_TRACE = """
            java.lang.IllegalStateException: Could not import orders-2026-10.csv
                at com.example.shop.OrderImporter.importFile(OrderImporter.java:48)
                at com.example.shop.Main.main(Main.java:15)
            Caused by: java.lang.IllegalArgumentException: Bad price in line 7: "12,000"
                at com.example.shop.CsvOrderParser.parsePrice(CsvOrderParser.java:63)
                at com.example.shop.CsvOrderParser.parseLine(CsvOrderParser.java:41)
                at com.example.shop.OrderImporter.importFile(OrderImporter.java:44)
                ... 1 more
            Caused by: java.lang.NumberFormatException: For input string: "12,000"
                at java.base/java.lang.NumberFormatException.forInputString(NumberFormatException.java:67)
                at java.base/java.lang.Long.parseLong(Long.java:594)
                at java.base/java.lang.Long.parseLong(Long.java:698)
                at com.example.shop.CsvOrderParser.parsePrice(CsvOrderParser.java:61)
                ... 3 more
            """;

    /** Where a value lives while a method runs. */
    enum Where {
        STACK,
        HEAP
    }

    /** How a program run in its own JVM ended. */
    enum Ending {
        COMPLETES,
        STACK_OVERFLOW_ERROR,
        OUT_OF_MEMORY_ERROR
    }
}
