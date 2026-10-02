package practice.j01.ex02;

import practice.j01.ex02.Puzzles.Outcome;

/**
 * Your answer sheet for J01-02. Replace each {@code null} with what you predict the matching method in
 * {@link Puzzles} returns, then run {@code PuzzlesTest}.
 *
 * <p>The constant's type tells you what kind of literal to write. For example {@code 3} for an
 * {@code Integer}, {@code 3L} for a {@code Long}, {@code 'x'} for a {@code Character}, {@code "x"} for a
 * {@code String}, {@code true}/{@code false} for a {@code Boolean}, and {@code Outcome.RETURNS_NORMALLY} for an
 * {@code Outcome}. If the compiler complains about a literal, that's a clue, too.
 */
final class Answers {

    private Answers() {
    }

    // Numbers

    // int / int is integer division: the fraction is cut off (not rounded). Unlike TypeScript, where 7 / 2 is 3.5.
    static final Integer INTEGER_DIVISION = 3;
    // % keeps the sign of the left operand. Use Math.floorMod(-7, 3) == 2 when you need a non-negative result.
    static final Integer REMAINDER_OF_NEGATIVE = -1;
    // int arithmetic wraps around silently. Math.addExact would throw ArithmeticException instead.
    static final Integer OVERFLOW = Integer.MIN_VALUE;
    // All operands are ints, so the multiplication overflows *before* the result is widened to long.
    // Write 24L * 60 * ... to do the math in long. (The literal needs an L: 500654080 alone is an int.)
    static final Long MULTIPLY_BEFORE_WIDENING = 500_654_080L;
    // Binary floating point can't represent 0.1 or 0.2 exactly. Use BigDecimal for money (J07-03).
    static final Double FLOATING_POINT = 0.30000000000000004;
    // A cast to int drops the fraction toward zero, so -3.99 becomes -3 (not -4).
    static final Integer CAST_TRUNCATES = -3;
    // Floating-point division by zero gives Infinity (or NaN for 0.0 / 0), never an exception.
    static final Double DOUBLE_DIVISION_BY_ZERO = Double.POSITIVE_INFINITY;
    // Integer division by zero has no sensible result, so it throws.
    static final Outcome INT_DIVISION_BY_ZERO = Outcome.THROWS_ARITHMETIC_EXCEPTION;

    // Characters and strings

    // A char is a 16-bit number. 'a' is 97, so 'a' + 1 is the int 98.
    static final Integer CHAR_PLUS_INT = 98;
    // Compound assignment (+=) includes a hidden cast back to char. Note: letter = letter + 2 would not compile.
    static final Character CHAR_COMPOUND_ASSIGNMENT = 'c';
    // + is evaluated left to right: 1 + 2 is 3 (numbers), then everything after the first String is concatenation.
    static final String STRING_CONCATENATION_ORDER = "3345";
    // Strings are immutable: toUpperCase() returns a new String, which the puzzle throws away.
    static final String STRINGS_ARE_IMMUTABLE = "java";

    // Boxing and identity

    // == compares references. Integer.valueOf caches -128..127, so both variables point to the same object...
    static final Boolean SMALL_INTEGERS_COMPARE_WITH_DOUBLE_EQUALS = true;
    // ...but 128 is outside the cache, so these are two different objects. Always compare objects with equals().
    static final Boolean LARGE_INTEGERS_COMPARE_WITH_DOUBLE_EQUALS = false;
    // new String(...) always creates a new object, even though the text is the same.
    static final Boolean NEW_STRING_COMPARE_WITH_DOUBLE_EQUALS = false;
    // 1 is boxed to an Integer, and Long.equals is only true for another Long.
    static final Boolean LONG_EQUALS_INTEGER = false;
    // Unboxing calls count.intValue() for you, and that call fails on null.
    static final Outcome UNBOXING_NULL = Outcome.THROWS_NULL_POINTER_EXCEPTION;

    // Passing values to methods

    // Java always passes a copy of the value. For an int, the method changes its own copy only.
    static final Integer PRIMITIVE_ARGUMENT = 1;
    // For objects, the value that is copied is the reference. Both references point to the same StringBuilder.
    static final String MUTATE_THROUGH_REFERENCE = "ab";
    // Assigning a new object to the parameter only changes the method's copy of the reference.
    static final String REASSIGN_PARAMETER = "a";
}
