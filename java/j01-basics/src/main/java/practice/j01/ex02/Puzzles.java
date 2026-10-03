package practice.j01.ex02;

/**
 * J01-02 · Predict: types, values and references.
 *
 * <p>Each method is a small puzzle. <strong>Don't run anything yet.</strong> Read each puzzle, decide what it
 * returns, and write your prediction in {@link Answers}. Then run the tests: a test fails when a prediction
 * was wrong, and the module README explains every answer.
 */
final class Puzzles {

    private Puzzles() {
    }

    // ---- Numbers -------------------------------------------------------------------------------

    static int integerDivision() {
        return 7 / 2;
    }

    static int remainderOfNegative() {
        return -7 % 3;
    }

    static int overflow() {
        return Integer.MAX_VALUE + 1;
    }

    static boolean multiplyBeforeWidening() {
        long microsPerDay = 24 * 60 * 60 * 1000 * 1000;
        return microsPerDay == 86_400_000_000L;
    }

    static double floatingPoint() {
        return 0.1 + 0.2;
    }

    static int castTruncates() {
        return (int) -3.99;
    }

    static double doubleDivisionByZero() {
        return 1.0 / 0;
    }

    static Outcome intDivisionByZero() {
        try {
            int zero = 0;
            int result = 1 / zero;
            return Outcome.RETURNS_NORMALLY;
        } catch (ArithmeticException e) {
            return Outcome.THROWS_ARITHMETIC_EXCEPTION;
        }
    }

    // ---- Characters and strings ----------------------------------------------------------------

    static int charPlusInt() {
        return 'a' + 1;
    }

    static char charCompoundAssignment() {
        char letter = 'a';
        letter += 2;
        return letter;
    }

    static String stringConcatenationOrder() {
        return 1 + 2 + "3" + 4 + 5;
    }

    static String stringsAreImmutable() {
        String language = "java";
        language.toUpperCase();
        return language;
    }

    // ---- Boxing and identity -------------------------------------------------------------------

    static boolean smallIntegersCompareWithDoubleEquals() {
        Integer a = 127;
        Integer b = 127;
        return a == b;
    }

    static boolean largeIntegersCompareWithDoubleEquals() {
        Integer a = 128;
        Integer b = 128;
        return a == b;
    }

    static boolean newStringCompareWithDoubleEquals() {
        String a = "hello";
        String b = new String("hello");
        return a == b;
    }

    static boolean longEqualsInteger() {
        Long big = 1L;
        return big.equals(1);
    }

    static Outcome unboxingNull() {
        try {
            Integer count = null;
            int copy = count;
            return Outcome.RETURNS_NORMALLY;
        } catch (NullPointerException e) {
            return Outcome.THROWS_NULL_POINTER_EXCEPTION;
        }
    }

    // ---- Passing values to methods -------------------------------------------------------------

    static int primitiveArgument() {
        int number = 1;
        addOne(number);
        return number;
    }

    static String mutateThroughReference() {
        StringBuilder text = new StringBuilder("a");
        appendB(text);
        return text.toString();
    }

    static String reassignParameter() {
        StringBuilder text = new StringBuilder("a");
        replaceWithB(text);
        return text.toString();
    }

    private static void addOne(int number) {
        number = number + 1;
    }

    private static void appendB(StringBuilder text) {
        text.append("b");
    }

    private static void replaceWithB(StringBuilder text) {
        text = new StringBuilder("b");
    }

    /** How a puzzle ends: with a value, or with an exception. */
    enum Outcome {
        RETURNS_NORMALLY,
        THROWS_ARITHMETIC_EXCEPTION,
        THROWS_NULL_POINTER_EXCEPTION
    }
}
