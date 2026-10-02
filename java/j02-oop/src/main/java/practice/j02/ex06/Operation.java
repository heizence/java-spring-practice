package practice.j02.ex06;

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

    Operation(String symbol) {
        // TODO: J02-06 (keep the symbol in a field. This constructor runs once per constant, when the enum loads,
        // so it must not throw.)
    }

    /** The symbol, such as {@code "+"}. */
    public String symbol() {
        // TODO: J02-06
        throw new UnsupportedOperationException("TODO: J02-06");
    }

    /** Applies this operation: {@code left + right}, {@code left - right}, and so on. */
    public long apply(long left, long right) {
        // TODO: J02-06
        throw new UnsupportedOperationException("TODO: J02-06");
    }

    /** The operation with this symbol. */
    public static Operation fromSymbol(String symbol) {
        // TODO: J02-06
        throw new UnsupportedOperationException("TODO: J02-06");
    }
}
