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
        // Record patterns take a record apart: "case Add(Expr left, Expr right)" matches an Add and binds its two
        // components in one step.
        return switch (this) {
            case Num(long value) -> value;
            case Add(Expr left, Expr right) -> Math.addExact(left.eval(), right.eval());
            case Mul(Expr left, Expr right) -> Math.multiplyExact(left.eval(), right.eval());
            case Neg(Expr operand) -> Math.negateExact(operand.eval());
        };
    }

    /**
     * Returns an expression with the same value, simplified with exactly these rules:
     * <ul>
     *   <li>{@code x * 1} and {@code 1 * x} become {@code x};</li>
     *   <li>{@code x + 0} and {@code 0 + x} become {@code x};</li>
     *   <li>{@code Neg(Neg(x))} becomes {@code x}.</li>
     * </ul>
     * The rules apply everywhere in the tree, until none applies any more. Simplify the children first, then
     * apply the rules to the node built from them: {@code (0 * 1) + 5} becomes {@code 0 + 5}, then {@code 5}.
     * Nothing else changes: {@code x * 0}, {@code Neg(Num(0))} and {@code 2 + 3} stay as they are.
     */
    default Expr simplify() {
        // Step 1: simplify the children (bottom-up).
        Expr node = switch (this) {
            case Num num -> num;
            case Add(Expr left, Expr right) -> new Add(left.simplify(), right.simplify());
            case Mul(Expr left, Expr right) -> new Mul(left.simplify(), right.simplify());
            case Neg(Expr operand) -> new Neg(operand.simplify());
        };
        // Step 2: apply the rules to this node. Nested record patterns look two levels deep at once, for example
        // "an Add whose right child is a Num". Java has no constant patterns such as Num(0) yet, so a "when" guard
        // checks the number. A rule returns a child, which step 1 already simplified, so one pass is enough.
        return switch (node) {
            case Add(Expr left, Num(long value)) when value == 0 -> left;
            case Add(Num(long value), Expr right) when value == 0 -> right;
            case Mul(Expr left, Num(long value)) when value == 1 -> left;
            case Mul(Num(long value), Expr right) when value == 1 -> right;
            case Neg(Neg(Expr inner)) -> inner;
            // No rule matched. Listing the four types instead of writing "default" keeps the switch exhaustive:
            // a new kind of Expr would make it fail to compile.
            case Num _, Add _, Mul _, Neg _ -> node;
        };
    }
}
