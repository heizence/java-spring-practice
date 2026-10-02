package practice.j01.ex03;

/**
 * Small string helpers of the kind that every code base has somewhere.
 *
 * <p>Every method throws {@link NullPointerException} when an argument is {@code null}.
 */
public final class StringTools {

    private StringTools() {
    }

    /**
     * Returns whether {@code text} reads the same forwards and backwards, looking only at its letters and digits
     * and ignoring letter case.
     *
     * <ul>
     *   <li>Letters and digits are the characters for which {@link Character#isLetterOrDigit(char)} is true.
     *       Everything else, such as spaces and punctuation, is skipped.</li>
     *   <li>Korean syllables are letters, so {@code "기러기"} is a palindrome.</li>
     *   <li>Text without any letters or digits, including the empty string, counts as a palindrome.</li>
     * </ul>
     *
     * <p>Examples: {@code "A man, a plan, a canal: Panama!"} → {@code true}; {@code "Java"} → {@code false}.
     */
    public static boolean isPalindrome(String text) {
        // TODO: J01-03
        throw new UnsupportedOperationException("TODO: J01-03");
    }

    /**
     * Makes the first character of every word uppercase and leaves all other characters as they are.
     *
     * <ul>
     *   <li>A word starts at the beginning of the text and after every whitespace character
     *       ({@link Character#isWhitespace(char)}).</li>
     *   <li>Whitespace is kept exactly as it is: double spaces, tabs and line breaks stay where they are, also at
     *       the start and the end of the text.</li>
     *   <li>The other characters of a word don't change, so {@code "API"} stays {@code "API"}.</li>
     *   <li>Characters without letter case, such as Korean syllables and digits, stay as they are.</li>
     * </ul>
     *
     * <p>Examples: {@code "hello world"} → {@code "Hello World"}; {@code "the API is ready"} →
     * {@code "The API Is Ready"}; {@code "안녕 world"} → {@code "안녕 World"}; {@code ""} → {@code ""}.
     */
    public static String capitalizeWords(String text) {
        // TODO: J01-03
        throw new UnsupportedOperationException("TODO: J01-03");
    }

    /**
     * Hides most of an email address, so it can be shown on screen, for example on a page that says
     * "We sent a code to m***@example.com".
     *
     * <ul>
     *   <li>Keeps the first character of the address, then writes exactly three asterisks, then the {@code @}
     *       and everything after it: {@code "minji.kim@example.com"} → {@code "m***@example.com"}.</li>
     *   <li>It's always three asterisks, so the result doesn't reveal how long the name is:
     *       {@code "a@example.com"} → {@code "a***@example.com"}.</li>
     *   <li>The address must contain exactly one {@code @}, with at least one character before it and at least
     *       one character after it. Otherwise the method throws {@link IllegalArgumentException}.</li>
     *   <li>The exception message must <em>not</em> contain the address: exception messages end up in log files,
     *       and an email address is personal data.</li>
     * </ul>
     */
    public static String maskEmail(String email) {
        // TODO: J01-03
        throw new UnsupportedOperationException("TODO: J01-03");
    }

    /**
     * Formats one line of a receipt: the item name, the quantity, and the line total.
     *
     * <pre>
     * receiptLine("Americano", 2, 450) returns
     * "Americano              2       9.00"
     *  |- 20 characters --| |3| |-- 10 --|
     * </pre>
     *
     * <ul>
     *   <li>The name is left-aligned in a column of 20 characters. A longer name is cut to its first 17
     *       characters followed by {@code "..."}, so the column stays 20 characters wide.</li>
     *   <li>Then a space, the quantity right-aligned in 3 characters, another space, and the line total
     *       right-aligned in 10 characters. If the total needs more than 10 characters, the column grows.</li>
     *   <li>The line total is {@code quantity × unitPriceCents}, written as units, a point, and two digits for the
     *       cents: 900 → {@code "9.00"}, 5 → {@code "0.05"}, 123456 → {@code "1234.56"}.</li>
     *   <li>{@code quantity} must be between 1 and 999, and {@code unitPriceCents} must not be negative.
     *       Otherwise the method throws {@link IllegalArgumentException}.</li>
     *   <li>If the line total is too large for a {@code long}, the method throws {@link ArithmeticException}
     *       instead of returning a wrong number.</li>
     * </ul>
     */
    public static String receiptLine(String item, int quantity, long unitPriceCents) {
        // TODO: J01-03
        throw new UnsupportedOperationException("TODO: J01-03");
    }

    /**
     * Returns the text of an order confirmation email. Write it with a text block ({@code """}).
     *
     * <p>For {@code orderConfirmation("Minji", "A-1001")} the result is these four lines:
     *
     * <pre>
     * Hi Minji,
     *
     * thank you for your order A-1001.
     * We'll email you again when it ships.
     * </pre>
     *
     * <ul>
     *   <li>Every line ends with {@code \n}, including the last one. No line starts with spaces.</li>
     *   <li>{@code customer} and {@code orderId} are inserted exactly as they are, even when they contain
     *       characters such as {@code %}.</li>
     * </ul>
     */
    public static String orderConfirmation(String customer, String orderId) {
        // TODO: J01-03
        throw new UnsupportedOperationException("TODO: J01-03");
    }
}
