package practice.j01.ex03;

import java.util.Objects;

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
        Objects.requireNonNull(text, "text");
        var kept = new StringBuilder(text.length());
        for (char c : text.toCharArray()) {
            if (Character.isLetterOrDigit(c)) {
                kept.append(Character.toLowerCase(c));
            }
        }
        String forwards = kept.toString();
        String backwards = kept.reverse().toString(); // reverse() changes the builder itself and returns it
        // Compare the contents with equals(). == would only check whether both are the same String object,
        // and toString() creates a new object every time.
        return forwards.equals(backwards);
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
        Objects.requireNonNull(text, "text");
        var result = new StringBuilder(text.length());
        boolean atWordStart = true;
        // One pass over the characters, remembering whether the previous one was whitespace. Splitting on " "
        // would lose trailing spaces and treat tabs and line breaks as part of a word.
        for (char c : text.toCharArray()) {
            if (Character.isWhitespace(c)) {
                result.append(c);
                atWordStart = true;
            } else if (atWordStart) {
                result.append(Character.toUpperCase(c));
                atWordStart = false;
            } else {
                result.append(c);
            }
        }
        return result.toString();
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
        Objects.requireNonNull(email, "email");
        int at = email.indexOf('@');
        // indexOf finds the first @ and lastIndexOf the last one. If they differ, there is more than one.
        boolean valid = at > 0 && at == email.lastIndexOf('@') && at < email.length() - 1;
        if (!valid) {
            // Describe the problem, not the value: the address is personal data, and logs are not private.
            throw new IllegalArgumentException("not a valid email address: it needs exactly one @ with text on "
                    + "both sides");
        }
        return email.charAt(0) + "***" + email.substring(at);
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
        Objects.requireNonNull(item, "item");
        if (quantity < 1 || quantity > 999) {
            throw new IllegalArgumentException("quantity must be between 1 and 999, but was " + quantity);
        }
        if (unitPriceCents < 0) {
            throw new IllegalArgumentException("unitPriceCents must not be negative, but was " + unitPriceCents);
        }
        String name = item.length() > 20 ? item.substring(0, 17) + "..." : item;
        // multiplyExact throws ArithmeticException on overflow, where * would silently wrap around.
        long totalCents = Math.multiplyExact(unitPriceCents, quantity);
        // The money stays in whole cents (a long), so there are no floating-point rounding errors. Integer division
        // and % split it into units and cents, and %02d pads the cents to two digits.
        String total = "%d.%02d".formatted(totalCents / 100, totalCents % 100);
        // %-20s: left-aligned in 20 characters. %3d and %10s: right-aligned in 3 and 10 characters.
        return "%-20s %3d %10s".formatted(name, quantity, total);
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
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(orderId, "orderId");
        return """
                Hi %s,

                thank you for your order %s.
                We'll email you again when it ships.""".formatted(customer, orderId);
    }
}
