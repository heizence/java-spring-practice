package practice.j01.ex02;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.testkit.Predict;

@DisplayName("J01-02 · Predict: types, values and references")
class PuzzlesTest {

    @Nested
    @DisplayName("Numbers")
    class Numbers {

        @Test
        @DisplayName("7 / 2")
        void integerDivision() {
            Predict.check(Answers.INTEGER_DIVISION, Puzzles.integerDivision());
        }

        @Test
        @DisplayName("-7 % 3")
        void remainderOfNegative() {
            Predict.check(Answers.REMAINDER_OF_NEGATIVE, Puzzles.remainderOfNegative());
        }

        @Test
        @DisplayName("Integer.MAX_VALUE + 1")
        void overflow() {
            Predict.check(Answers.OVERFLOW, Puzzles.overflow());
        }

        @Test
        @DisplayName("24 * 60 * 60 * 1000 * 1000 stored in a long == 86_400_000_000L")
        void multiplyBeforeWidening() {
            Predict.check(Answers.MULTIPLY_BEFORE_WIDENING, Puzzles.multiplyBeforeWidening());
        }

        @Test
        @DisplayName("0.1 + 0.2")
        void floatingPoint() {
            Predict.check(Answers.FLOATING_POINT, Puzzles.floatingPoint());
        }

        @Test
        @DisplayName("(int) -3.99")
        void castTruncates() {
            Predict.check(Answers.CAST_TRUNCATES, Puzzles.castTruncates());
        }

        @Test
        @DisplayName("1.0 / 0")
        void doubleDivisionByZero() {
            Predict.check(Answers.DOUBLE_DIVISION_BY_ZERO, Puzzles.doubleDivisionByZero());
        }

        @Test
        @DisplayName("1 / 0 with ints")
        void intDivisionByZero() {
            Predict.check(Answers.INT_DIVISION_BY_ZERO, Puzzles.intDivisionByZero());
        }
    }

    @Nested
    @DisplayName("Characters and strings")
    class CharactersAndStrings {

        @Test
        @DisplayName("'a' + 1")
        void charPlusInt() {
            Predict.check(Answers.CHAR_PLUS_INT, Puzzles.charPlusInt());
        }

        @Test
        @DisplayName("char letter = 'a'; letter += 2")
        void charCompoundAssignment() {
            Predict.check(Answers.CHAR_COMPOUND_ASSIGNMENT, Puzzles.charCompoundAssignment());
        }

        @Test
        @DisplayName("1 + 2 + \"3\" + 4 + 5")
        void stringConcatenationOrder() {
            Predict.check(Answers.STRING_CONCATENATION_ORDER, Puzzles.stringConcatenationOrder());
        }

        @Test
        @DisplayName("calling toUpperCase() without using the result")
        void stringsAreImmutable() {
            Predict.check(Answers.STRINGS_ARE_IMMUTABLE, Puzzles.stringsAreImmutable());
        }
    }

    @Nested
    @DisplayName("Boxing and identity")
    class BoxingAndIdentity {

        @Test
        @DisplayName("Integer 127 == Integer 127")
        void smallIntegers() {
            Predict.check(Answers.SMALL_INTEGERS_COMPARE_WITH_DOUBLE_EQUALS, Puzzles.smallIntegersCompareWithDoubleEquals());
        }

        @Test
        @DisplayName("Integer 128 == Integer 128")
        void largeIntegers() {
            Predict.check(Answers.LARGE_INTEGERS_COMPARE_WITH_DOUBLE_EQUALS, Puzzles.largeIntegersCompareWithDoubleEquals());
        }

        @Test
        @DisplayName("\"hello\" == new String(\"hello\")")
        void newString() {
            Predict.check(Answers.NEW_STRING_COMPARE_WITH_DOUBLE_EQUALS, Puzzles.newStringCompareWithDoubleEquals());
        }

        @Test
        @DisplayName("Long 1L equals Integer 1")
        void longEqualsInteger() {
            Predict.check(Answers.LONG_EQUALS_INTEGER, Puzzles.longEqualsInteger());
        }

        @Test
        @DisplayName("unboxing a null Integer")
        void unboxingNull() {
            Predict.check(Answers.UNBOXING_NULL, Puzzles.unboxingNull());
        }
    }

    @Nested
    @DisplayName("Passing values to methods")
    class PassingValues {

        @Test
        @DisplayName("a method changes its int parameter")
        void primitiveArgument() {
            Predict.check(Answers.PRIMITIVE_ARGUMENT, Puzzles.primitiveArgument());
        }

        @Test
        @DisplayName("a method appends to the StringBuilder it receives")
        void mutateThroughReference() {
            Predict.check(Answers.MUTATE_THROUGH_REFERENCE, Puzzles.mutateThroughReference());
        }

        @Test
        @DisplayName("a method assigns a new StringBuilder to its parameter")
        void reassignParameter() {
            Predict.check(Answers.REASSIGN_PARAMETER, Puzzles.reassignParameter());
        }
    }
}
