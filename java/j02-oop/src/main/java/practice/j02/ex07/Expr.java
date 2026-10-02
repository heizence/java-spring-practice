package practice.j02.ex07;

import java.util.Objects;

/**
 * An arithmetic expression on whole numbers, stored as a tree. For example, {@code (2 + 3) * -4} is
 * {@code new Mul(new Add(new Num(2), new Num(3)), new Neg(new Num(4)))}.
 *
 * <p>The interface is {@code sealed} and its four records are the only kinds of node, so a {@code switch} over
 * an {@code Expr} can be exhaustive without a {@code default} branch. The records reject {@code null} children with
 * {@link NullPointerException}. Records compare by value, so two trees with the same shape and numbers are
 * {@code equals}.
 *
 * <p>The records are finished. Your part is {@link #eval()} and {@link #simplify()}.
 */
public sealed interface Expr {

    /** A number. */
    record Num(long value) implements Expr {
    }

    /** {@code left + right}. */
    record Add(Expr left, Expr right) implements Expr {

        public Add {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }
    }

    /** {@code left * right}. */
    record Mul(Expr left, Expr right) implements Expr {

        public Mul {
            Objects.requireNonNull(left, "left");
            Objects.requireNonNull(right, "right");
        }
    }

    /** {@code -operand}. */
    record Neg(Expr operand) implements Expr {

        public Neg {
            Objects.requireNonNull(operand, "operand");
        }
    }

    /**
     * Computes the value of this expression. Uses exact {@code long} arithmetic: a result that doesn't fit in a
     * {@code long} throws {@link ArithmeticException}.
     */
    default long eval() {
        // TODO: J02-07
        throw new UnsupportedOperationException("TODO: J02-07");
    }

    /**
     * Returns an expression with the same value, simplified with exactly these rules:
     * <ul>
     *   <li>{@code x * 1} and {@code 1 * x} become {@code x};</li>
     *   <li>{@code x + 0} and {@code 0 + x} become {@code x};</li>
     *   <li>{@code Neg(Neg(x))} becomes {@code x}.</li>
     * </ul>
     * The rules apply everywhere in the tree, until none applies anymore. Simplify the children first, then
     * apply the rules to the node built from them: {@code (0 * 1) + 5} becomes {@code 0 + 5}, then {@code 5}.
     * Nothing else changes: {@code x * 0}, {@code Neg(Num(0))} and {@code 2 + 3} stay as they are.
     */
    default Expr simplify() {
        // TODO: J02-07
        throw new UnsupportedOperationException("TODO: J02-07");
    }
}
