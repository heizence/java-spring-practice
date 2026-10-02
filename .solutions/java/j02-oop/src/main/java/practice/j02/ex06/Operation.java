package practice.j02.ex06;

import java.util.Objects;

/**
 * The four basic operations of a calculator for whole numbers.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #symbol()} is {@code "+"}, {@code "-"}, {@code "*"} or {@code "/"}.</li>
 *   <li>{@link #apply} computes with {@code long} values. A result that doesn't fit in a {@code long} throws
 *       {@link ArithmeticException} instead of wrapping around; this includes {@code Long.MIN_VALUE / -1}.</li>
 *   <li>Division rounds toward zero, like the {@code /} operator: {@code -7 / 2} is {@code -3}. Dividing by 0 throws
 *       {@link ArithmeticException}.</li>
 *   <li>{@link #fromSymbol} returns the operation with exactly that symbol. Any other string, including
 *       {@code ""} and symbols with spaces around them, throws {@link IllegalArgumentException} with the string in
 *       the message. {@code null} throws {@link NullPointerException}.</li>
 * </ul>
 */
public enum Operation {
    PLUS("+"),
    MINUS("-"),
    TIMES("*"),
    DIVIDE("/");

    private final String symbol;

    // Enum constructors are always private: no code outside the enum can create more constants.
    Operation(String symbol) {
        this.symbol = symbol;
    }

    /** The symbol, such as {@code "+"}. */
    public String symbol() {
        return symbol;
    }

    /** Applies this operation: {@code left + right}, {@code left - right}, and so on. */
    public long apply(long left, long right) {
        // The ...Exact methods throw ArithmeticException on overflow, where +, -, * and / would wrap around.
        // No default branch: if someone adds a constant, this switch stops compiling until they handle it.
        return switch (this) {
            case PLUS -> Math.addExact(left, right);
            case MINUS -> Math.subtractExact(left, right);
            case TIMES -> Math.multiplyExact(left, right);
            case DIVIDE -> Math.divideExact(left, right); // also throws for division by 0
        };
    }

    /** The operation with this symbol. */
    public static Operation fromSymbol(String symbol) {
        Objects.requireNonNull(symbol, "symbol");
        // A loop is fine for four constants. With many constants, build a Map<String, Operation> once, in a
        // static field.
        for (Operation operation : values()) {
            if (operation.symbol.equals(symbol)) {
                return operation;
            }
        }
        throw new IllegalArgumentException("unknown operation symbol: \"" + symbol + "\"");
    }
}
