package practice.j02.ex02;

import java.util.Objects;

/**
 * An ISBN-13: the 13-digit number printed above a book's barcode.
 *
 * <p>{@code Isbn} is a <em>value object</em>: what matters is the number it holds, not which object holds it. Two
 * {@code Isbn} objects are equal when they hold the same 13 digits, however the input was written.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The constructor removes every hyphen ({@code -}) and every space from the input. So
 *       {@code "978-0-306-40615-7"}, {@code "978 0 306 40615 7"} and {@code "9780306406157"} are the same ISBN.
 *       No other characters are removed.</li>
 *   <li>What is left must be exactly 13 ASCII digits ({@code 0} to {@code 9}) with a valid check digit (next rule).
 *       Otherwise the constructor throws {@link IllegalArgumentException} with the input in its message.
 *       {@code null} throws {@link NullPointerException}.</li>
 *   <li>Check digit: multiply the 13 digits alternately by 1 and 3 (the first digit by 1, the second by 3, the
 *       third by 1, and so on, up to the last digit) and add the results. The ISBN is valid when the sum is a
 *       multiple of 10.</li>
 *   <li>{@link #equals} and {@link #hashCode} use only the 13 digits. {@code equals} keeps the full contract of
 *       {@link Object#equals}: reflexive, symmetric, transitive, consistent, and {@code false} for {@code null} or
 *       an object of another type (such as a {@code String} with the same digits).</li>
 *   <li>{@link #toString()} returns {@code "ISBN "} followed by the 13 digits, for example
 *       {@code "ISBN 9780306406157"}.</li>
 * </ul>
 */
public final class Isbn {

    private static final int LENGTH = 13;

    private final String digits;

    /** Parses {@code text}, which may contain hyphens and spaces. */
    public Isbn(String text) {
        Objects.requireNonNull(text, "text");
        // Normalize first, then validate and store only the normalized form. equals, hashCode and toString can
        // then work with one canonical spelling and never need to know how the input was written.
        String normalized = text.replace("-", "").replace(" ", "");
        if (!isThirteenAsciiDigits(normalized) || !hasValidCheckDigit(normalized)) {
            throw new IllegalArgumentException("not a valid ISBN-13: \"" + text + "\"");
        }
        this.digits = normalized;
    }

    /** The 13 digits, without hyphens or spaces. */
    public String digits() {
        return digits;
    }

    @Override
    public boolean equals(Object other) {
        // "instanceof with a pattern" checks the type and casts in one step. It is false for null, which covers
        // the null rule. The class is final, so no subclass can break symmetry.
        return other instanceof Isbn that && digits.equals(that.digits);
    }

    @Override
    public int hashCode() {
        // Equal objects must have equal hash codes, so hash exactly what equals compares.
        return digits.hashCode();
    }

    @Override
    public String toString() {
        return "ISBN " + digits;
    }

    private static boolean isThirteenAsciiDigits(String text) {
        if (text.length() != LENGTH) {
            return false;
        }
        for (int i = 0; i < LENGTH; i++) {
            char c = text.charAt(i);
            if (!Character.isDigit(c)) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasValidCheckDigit(String digits) {
        int sum = 0;
        for (int i = 0; i < LENGTH; i++) {
            int digit = Character.getNumericValue(digits.charAt(i));
            sum += i % 2 == 0 ? digit : 3 * digit;
        }
        return sum % 10 == 0;
    }
}
