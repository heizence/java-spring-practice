package practice.j05.ex03;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;

/**
 * Checks whether the brackets in a text are balanced, the way a code editor or a JSON parser does.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The brackets are {@code ()}, {@code []} and {@code {}}. Every other character is ignored.</li>
 *   <li>Balanced means that every opening bracket is closed by a closing bracket of the same type, and that
 *       the bracket opened last is closed first: {@code "([]{})"} is balanced, {@code "([)]"}, {@code "(]"},
 *       {@code "(()"} and {@code ")("} are not.</li>
 *   <li>A text without brackets, including the empty string, is balanced.</li>
 *   <li>{@code null} is rejected with {@link NullPointerException}.</li>
 *   <li>Use an {@link java.util.ArrayDeque} as the stack.</li>
 * </ul>
 */
public final class Brackets {

    private Brackets() {
    }

    /** Whether the brackets in {@code text} are balanced. */
    public static boolean isBalanced(String text) {
        Objects.requireNonNull(text, "text");
        // For every bracket that is still open, the stack holds the closing bracket it needs.
        // The innermost open bracket is on top, so it must be closed first.
        Deque<Character> expectedClosers = new ArrayDeque<>();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '(' -> expectedClosers.push(')');
                case '[' -> expectedClosers.push(']');
                case '{' -> expectedClosers.push('}');
                case ')', ']', '}' -> {
                    // A closing bracket with nothing open, or the wrong type, can never be repaired later.
                    if (expectedClosers.isEmpty() || expectedClosers.pop() != c) {
                        return false;
                    }
                }
                default -> {
                    // Not a bracket: ignore it.
                }
            }
        }
        return true;
    }
}
