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
        // Count the open brackets of each type.
        int round = 0;
        int square = 0;
        int curly = 0;
        for (char c : text.toCharArray()) {
            switch (c) {
                case '(' -> round++;
                case ')' -> round--;
                case '[' -> square++;
                case ']' -> square--;
                case '{' -> curly++;
                case '}' -> curly--;
                default -> {
                    // Not a bracket: ignore it.
                }
            }
            if (round < 0 || square < 0 || curly < 0) {
                return false;
            }
        }
        return round == 0 && square == 0 && curly == 0;
    }
}
