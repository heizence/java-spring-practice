package practice.j01.ex01;

/**
 * Builds a greeting for a name and the time of day, such as {@code "Good morning, Minji!"}.
 *
 * <h2>Rules for {@link #format(String, int)}</h2>
 * <ul>
 *   <li>The result has the form {@code "<greeting>, <name>!"}.</li>
 *   <li>{@code hour} is an hour on a 24-hour clock. It chooses the greeting:
 *     <ul>
 *       <li>5 to 11: {@code "Good morning"}</li>
 *       <li>12 to 17: {@code "Good afternoon"}</li>
 *       <li>18 to 21: {@code "Good evening"}</li>
 *       <li>22, 23 and 0 to 4: {@code "Good night"}</li>
 *     </ul>
 *   </li>
 *   <li>An {@code hour} below 0 or above 23 throws {@link IllegalArgumentException}. The message contains the
 *       hour.</li>
 *   <li>Whitespace at the start and at the end of {@code name} is removed. Whitespace means every character for
 *       which {@link Character#isWhitespace(char)} is true: spaces, tabs, line breaks, and also the full-width
 *       space (U+3000) used in Chinese, Japanese and Korean text. Whitespace inside the name stays.</li>
 *   <li>If {@code name} is {@code null}, or nothing is left after removing the whitespace, the greeting uses
 *       {@code "there"} instead: {@code "Good morning, there!"}.</li>
 * </ul>
 */
public final class Greeting {

    private Greeting() {
        // Only static methods here, so nobody needs a Greeting object.
    }

    /**
     * Returns the greeting for {@code name} at {@code hour}. For example, {@code format("Minji", 9)} returns
     * {@code "Good morning, Minji!"}.
     *
     * @throws IllegalArgumentException if {@code hour} is not between 0 and 23
     */
    public static String format(String name, int hour) {
        // TODO: J01-01
        throw new UnsupportedOperationException("TODO: J01-01");
    }
}
