package practice.j10.ex01;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.testkit.Predict;

/**
 * Checks your answers in {@link Answers}.
 *
 * <p>The "How does the program end?" tests run each program in a new JVM and compare the real ending with your
 * prediction. No code can measure the other answers (where a value lives, whether an object is reachable, where to
 * look in a stack trace), so those tests compare your prediction with the answers in {@code Expected.java}. Don't
 * open that file before you predict.
 */
@DisplayName("J10-01 · Predict: stack, heap and errors")
class PuzzlesTest {

    @Nested
    @DisplayName("Stack or heap")
    class StackOrHeap {

        @Test
        @DisplayName("the parameter quantity")
        void parameter() {
            Predict.check(Answers.PARAMETER_QUANTITY, Expected.PARAMETER_QUANTITY);
        }

        @Test
        @DisplayName("the local variable unitPrice")
        void localVariable() {
            Predict.check(Answers.LOCAL_UNIT_PRICE, Expected.LOCAL_UNIT_PRICE);
        }

        @Test
        @DisplayName("the variable order")
        void referenceVariable() {
            Predict.check(Answers.ORDER_VARIABLE, Expected.ORDER_VARIABLE);
        }

        @Test
        @DisplayName("the Order object that order refers to")
        void object() {
            Predict.check(Answers.ORDER_OBJECT, Expected.ORDER_OBJECT);
        }

        @Test
        @DisplayName("the field id inside the Order object")
        void primitiveField() {
            Predict.check(Answers.ORDER_ID_FIELD, Expected.ORDER_ID_FIELD);
        }

        @Test
        @DisplayName("the numbers 5, 4 and 3 in the array")
        void arrayElements() {
            Predict.check(Answers.ARRAY_ELEMENTS, Expected.ARRAY_ELEMENTS);
        }

        @Test
        @DisplayName("the static field Order.created")
        void staticField() {
            Predict.check(Answers.STATIC_COUNTER, Expected.STATIC_COUNTER);
        }
    }

    @Nested
    @DisplayName("Garbage collection")
    class GarbageCollection {

        @Test
        @DisplayName("reassigned(): is Order 1 eligible?")
        void reassigned() {
            Predict.check(Answers.REASSIGNED, Expected.REASSIGNED);
        }

        @Test
        @DisplayName("secondReference(): is Order 1 eligible?")
        void secondReference() {
            Predict.check(Answers.SECOND_REFERENCE, Expected.SECOND_REFERENCE);
        }

        @Test
        @DisplayName("cycle(): are the two nodes eligible?")
        void cycle() {
            Predict.check(Answers.CYCLE, Expected.CYCLE);
        }

        @Test
        @DisplayName("staticList(): is Order 1 eligible?")
        void staticList() {
            Predict.check(Answers.STATIC_LIST, Expected.STATIC_LIST);
        }

        @Test
        @DisplayName("innerObject(): is the Report eligible?")
        void innerObject() {
            Predict.check(Answers.INNER_OBJECT, Expected.INNER_OBJECT);
        }
    }

    @Nested
    @DisplayName("How programs end")
    class HowProgramsEnd {

        @Test
        @DisplayName("DeepRecursion with -Xss512k")
        void deepRecursion() {
            Predict.check(Answers.DEEP_RECURSION, Puzzles.deepRecursion());
        }

        @Test
        @DisplayName("CyclicToString with -Xss512k")
        void cyclicToString() {
            Predict.check(Answers.CYCLIC_TO_STRING, Puzzles.cyclicToString());
        }

        @Test
        @DisplayName("GarbageInALoop with -Xmx16m")
        void garbageInALoop() {
            Predict.check(Answers.GARBAGE_IN_A_LOOP, Puzzles.garbageInALoop());
        }

        @Test
        @DisplayName("GrowingCache with -Xmx16m")
        void growingCache() {
            Predict.check(Answers.GROWING_CACHE, Puzzles.growingCache());
        }

        @Test
        @DisplayName("PrimitiveArray with -Xmx16m")
        void primitiveArray() {
            Predict.check(Answers.PRIMITIVE_ARRAY, Puzzles.primitiveArray());
        }

        @Test
        @DisplayName("BoxedList with -Xmx16m")
        void boxedList() {
            Predict.check(Answers.BOXED_LIST, Puzzles.boxedList());
        }
    }

    @Nested
    @DisplayName("Stack traces")
    class StackTraces {

        @Test
        @DisplayName("NullPointerException: where was it thrown?")
        void npeThrownAt() {
            Predict.check(Answers.NPE_THROWN_AT, Expected.NPE_THROWN_AT);
        }

        @Test
        @DisplayName("NullPointerException: where did the null come from?")
        void npeNullCameFrom() {
            Predict.check(Answers.NPE_NULL_CAME_FROM, Expected.NPE_NULL_CAME_FROM);
        }

        @Test
        @DisplayName("UnsupportedOperationException: which line of your code do you open first?")
        void firstLineOfYourCode() {
            Predict.check(Answers.FIRST_LINE_OF_YOUR_CODE, Expected.FIRST_LINE_OF_YOUR_CODE);
        }

        @Test
        @DisplayName("Caused by: where in your code did the original problem happen?")
        void rootCause() {
            Predict.check(Answers.ROOT_CAUSE_IN_YOUR_CODE, Expected.ROOT_CAUSE_IN_YOUR_CODE);
        }
    }
}
