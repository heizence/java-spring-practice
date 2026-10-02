package practice.j02.ex07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.j02.ex07.Expr.Add;
import practice.j02.ex07.Expr.Mul;
import practice.j02.ex07.Expr.Neg;
import practice.j02.ex07.Expr.Num;

@DisplayName("J02-07 · Records, sealed types and pattern matching")
class ExprTest {

    private static final Expr X = new Add(new Num(2), new Num(3)); // stands for "any expression" in the rules

    private static Num num(long value) {
        return new Num(value);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("eval() of a number is the number")
        void evalNum() {
            assertThat(num(42).eval()).isEqualTo(42);
        }

        @Test
        @DisplayName("eval() adds, multiplies and negates")
        void evalOperations() {
            assertThat(new Add(num(2), num(3)).eval()).isEqualTo(5);
            assertThat(new Mul(num(2), num(3)).eval()).isEqualTo(6);
            assertThat(new Neg(num(2)).eval()).isEqualTo(-2);
        }

        @Test
        @DisplayName("eval() works through a nested expression: (2 + 3) * -4 is -20")
        void evalNested() {
            var expression = new Mul(new Add(num(2), num(3)), new Neg(num(4)));

            assertThat(expression.eval()).isEqualTo(-20);
        }

        @Test
        @DisplayName("simplify() turns x * 1 and 1 * x into x")
        void timesOne() {
            assertThat(new Mul(X, num(1)).simplify()).isEqualTo(X);
            assertThat(new Mul(num(1), X).simplify()).isEqualTo(X);
        }

        @Test
        @DisplayName("simplify() turns x + 0 and 0 + x into x")
        void plusZero() {
            assertThat(new Add(X, num(0)).simplify()).isEqualTo(X);
            assertThat(new Add(num(0), X).simplify()).isEqualTo(X);
        }

        @Test
        @DisplayName("simplify() turns Neg(Neg(x)) into x")
        void doubleNegation() {
            assertThat(new Neg(new Neg(X)).simplify()).isEqualTo(X);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("simplify() works deep inside the tree")
        void deepInsideTheTree() {
            var expression = new Mul(new Add(num(7), num(0)), new Neg(num(3)));

            assertThat(expression.simplify()).isEqualTo(new Mul(num(7), new Neg(num(3))));
        }

        @Test
        @DisplayName("simplify() simplifies the children first: (0 * 1) + 5 becomes 5")
        void childrenFirst() {
            var expression = new Add(new Mul(num(0), num(1)), num(5));

            assertThat(expression.simplify()).isEqualTo(num(5));
        }

        @Test
        @DisplayName("simplify() uses a 1 that appears only after simplifying: Neg(Neg(1)) * x becomes x")
        void ruleAfterChildRule() {
            var expression = new Mul(new Neg(new Neg(num(1))), X);

            assertThat(expression.simplify()).isEqualTo(X);
        }

        @Test
        @DisplayName("simplify() removes negations in pairs")
        void negationPairs() {
            assertThat(new Neg(new Neg(new Neg(new Neg(X)))).simplify()).isEqualTo(X);
            assertThat(new Neg(new Neg(new Neg(X))).simplify()).isEqualTo(new Neg(X));
        }

        @Test
        @DisplayName("simplify() works when x itself is 0 or 1: 0 * 1 is 0 and 1 + 0 is 1")
        void ruleResultIsANumber() {
            assertThat(new Mul(num(0), num(1)).simplify()).isEqualTo(num(0));
            assertThat(new Add(num(1), num(0)).simplify()).isEqualTo(num(1));
            assertThat(new Add(num(0), num(0)).simplify()).isEqualTo(num(0));
        }

        @Test
        @DisplayName("simplify() leaves expressions that no rule matches unchanged")
        void noMatchingRule() {
            assertThat(new Mul(X, num(0)).simplify()).isEqualTo(new Mul(X, num(0)));
            assertThat(new Add(X, num(1)).simplify()).isEqualTo(new Add(X, num(1)));
            assertThat(new Neg(num(0)).simplify()).isEqualTo(new Neg(num(0)));
            assertThat(new Mul(num(2), num(2)).simplify()).isEqualTo(new Mul(num(2), num(2)));
            assertThat(new Add(num(-1), new Neg(num(1))).simplify()).isEqualTo(new Add(num(-1), new Neg(num(1))));
            assertThat(num(1).simplify()).isEqualTo(num(1));
        }

        @Test
        @DisplayName("eval() is fine right at the edges of the long range")
        void evalEdgesOfLongRange() {
            assertThat(new Add(num(Long.MAX_VALUE - 1), num(1)).eval()).isEqualTo(Long.MAX_VALUE);
            assertThat(new Neg(num(Long.MAX_VALUE)).eval()).isEqualTo(-Long.MAX_VALUE);
            assertThat(new Mul(num(Long.MIN_VALUE), num(1)).eval()).isEqualTo(Long.MIN_VALUE);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("eval() throws ArithmeticException when a result overflows")
        void evalOverflow() {
            assertThatThrownBy(() -> new Add(num(Long.MAX_VALUE), num(1)).eval())
                    .isInstanceOf(ArithmeticException.class);
            assertThatThrownBy(() -> new Mul(num(Long.MAX_VALUE), num(2)).eval())
                    .isInstanceOf(ArithmeticException.class);
            assertThatThrownBy(() -> new Neg(num(Long.MIN_VALUE)).eval())
                    .isInstanceOf(ArithmeticException.class);
        }

        @Test
        @DisplayName("an overflow deep inside the tree still throws")
        void evalOverflowDeepInside() {
            var expression = new Add(num(1), new Mul(num(Long.MIN_VALUE), num(-1)));

            assertThatThrownBy(expression::eval).isInstanceOf(ArithmeticException.class);
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("simplify() keeps the value of 1,000 random expressions")
        void keepsTheValue() {
            var random = new Random(42); // fixed seed: a failure is reproducible

            for (int step = 0; step < 1_000; step++) {
                Expr expression = randomExpr(random, 5);

                Expr simplified = expression.simplify();

                assertThat(simplified.eval())
                        .as("value of %s at step %d", expression, step)
                        .isEqualTo(expression.eval());
            }
        }

        @Test
        @DisplayName("simplify() leaves nothing to simplify in 1,000 random expressions")
        void leavesNothingToSimplify() {
            var random = new Random(42);

            for (int step = 0; step < 1_000; step++) {
                Expr expression = randomExpr(random, 5);

                Expr simplified = expression.simplify();

                assertThat(isFullySimplified(simplified))
                        .as("%s simplified to %s at step %d", expression, simplified, step)
                        .isTrue();
                assertThat(simplified.simplify()).as("simplifying twice at step %d", step).isEqualTo(simplified);
            }
        }

        /**
         * A random tree up to {@code depth} levels deep. The numbers are small (-2 to 2) so 0 and 1 are common, and
         * the depth limit keeps every value far from overflow.
         */
        private static Expr randomExpr(Random random, int depth) {
            if (depth == 0 || random.nextInt(4) == 0) {
                return new Num(random.nextInt(5) - 2);
            }
            return switch (random.nextInt(3)) {
                case 0 -> new Add(randomExpr(random, depth - 1), randomExpr(random, depth - 1));
                case 1 -> new Mul(randomExpr(random, depth - 1), randomExpr(random, depth - 1));
                default -> new Neg(randomExpr(random, depth - 1));
            };
        }

        /** {@code true} if no rule matches anywhere in the tree. */
        private static boolean isFullySimplified(Expr expr) {
            return switch (expr) {
                case Num _ -> true;
                case Add(Expr left, Expr right) ->
                        !isNum(left, 0) && !isNum(right, 0) && isFullySimplified(left) && isFullySimplified(right);
                case Mul(Expr left, Expr right) ->
                        !isNum(left, 1) && !isNum(right, 1) && isFullySimplified(left) && isFullySimplified(right);
                case Neg(Expr operand) -> !(operand instanceof Neg) && isFullySimplified(operand);
            };
        }

        private static boolean isNum(Expr expr, long value) {
            return expr instanceof Num(long actual) && actual == value;
        }
    }
}
