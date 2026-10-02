package practice.j01.ex01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J01-01 · Hello, Java")
class GreetingTest {

    /** What println() writes at the end of a line: "\n" on macOS and Linux, "\r\n" on Windows. */
    private static final String NEWLINE = System.lineSeparator();

    private final ByteArrayOutputStream out = new ByteArrayOutputStream();
    private final ByteArrayOutputStream err = new ByteArrayOutputStream();

    /**
     * Runs {@code GreetingApp.main(args)} while System.out and System.err write into the buffers {@code out} and
     * {@code err}. The real streams are put back afterwards, even when main throws.
     */
    private void runMain(String... args) {
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(err, true, StandardCharsets.UTF_8));
        try {
            GreetingApp.main(args);
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    private String printedToOut() {
        return out.toString(StandardCharsets.UTF_8);
    }

    private String printedToErr() {
        return err.toString(StandardCharsets.UTF_8);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("says \"Good morning\" at 9")
        void morning() {
            assertThat(Greeting.format("Minji", 9)).isEqualTo("Good morning, Minji!");
        }

        @Test
        @DisplayName("says \"Good afternoon\" at 14")
        void afternoon() {
            assertThat(Greeting.format("Minji", 14)).isEqualTo("Good afternoon, Minji!");
        }

        @Test
        @DisplayName("says \"Good evening\" at 19")
        void evening() {
            assertThat(Greeting.format("Minji", 19)).isEqualTo("Good evening, Minji!");
        }

        @Test
        @DisplayName("says \"Good night\" at 23")
        void night() {
            assertThat(Greeting.format("Minji", 23)).isEqualTo("Good night, Minji!");
        }

        @Test
        @DisplayName("main() prints the greeting for a name and an hour as one line")
        void mainPrintsGreeting() {
            runMain("Minji", "9");

            assertThat(printedToOut()).isEqualTo("Good morning, Minji!" + NEWLINE);
            assertThat(printedToErr()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @ParameterizedTest(name = "{0}:00 is {1}", quoteTextArguments = false)
        @CsvSource({
                "0, Good night",
                "4, Good night",
                "5, Good morning",
                "11, Good morning",
                "12, Good afternoon",
                "17, Good afternoon",
                "18, Good evening",
                "21, Good evening",
                "22, Good night",
                "23, Good night",
        })
        @DisplayName("switches the greeting at the right hours")
        void boundaries(int hour, String greeting) {
            assertThat(Greeting.format("Minji", hour)).isEqualTo(greeting + ", Minji!");
        }

        @Test
        @DisplayName("removes spaces, tabs and line breaks around the name")
        void stripsWhitespace() {
            assertThat(Greeting.format("  \tMinji \n", 9)).isEqualTo("Good morning, Minji!");
        }

        @Test
        @DisplayName("removes full-width spaces (U+3000) around the name, too")
        void stripsFullWidthSpaces() {
            // \u3000 is the full-width space, written as an escape because it looks like an ordinary space.
            assertThat(Greeting.format("\u3000민지\u3000", 9)).isEqualTo("Good morning, 민지!");
        }

        @Test
        @DisplayName("keeps the whitespace inside a name")
        void keepsInnerWhitespace() {
            assertThat(Greeting.format(" Mary Ann ", 9)).isEqualTo("Good morning, Mary Ann!");
        }

        @Test
        @DisplayName("uses \"there\" for a null name")
        void nullName() {
            assertThat(Greeting.format(null, 9)).isEqualTo("Good morning, there!");
        }

        @Test
        @DisplayName("uses \"there\" for an empty name")
        void emptyName() {
            assertThat(Greeting.format("", 9)).isEqualTo("Good morning, there!");
        }

        static Stream<Arguments> blankNames() {
            return Stream.of(
                    argumentSet("one space", " "),
                    argumentSet("a tab and a line break", "\t\n"),
                    argumentSet("two full-width spaces", "\u3000\u3000"));
        }

        @ParameterizedTest(name = "{argumentSetName}")
        @MethodSource("blankNames")
        @DisplayName("uses \"there\" for a name that is only whitespace")
        void blankName(String name) {
            assertThat(Greeting.format(name, 9)).isEqualTo("Good morning, there!");
        }

        @Test
        @DisplayName("main() prints the usage line to System.err when there are no arguments")
        void mainWithoutArguments() {
            runMain();

            assertThat(printedToErr()).isEqualTo(GreetingApp.USAGE + NEWLINE);
            assertThat(printedToOut()).isEmpty();
        }

        @Test
        @DisplayName("main() prints the usage line to System.err when the hour is missing")
        void mainWithOneArgument() {
            runMain("Minji");

            assertThat(printedToErr()).isEqualTo(GreetingApp.USAGE + NEWLINE);
            assertThat(printedToOut()).isEmpty();
        }

        @Test
        @DisplayName("main() prints the usage line to System.err for three arguments, like an unquoted Mary Ann")
        void mainWithThreeArguments() {
            runMain("Mary", "Ann", "20");

            assertThat(printedToErr()).isEqualTo(GreetingApp.USAGE + NEWLINE);
            assertThat(printedToOut()).isEmpty();
        }

        @Test
        @DisplayName("main() treats a quoted name with a space as one argument")
        void mainWithQuotedName() {
            runMain("Mary Ann", "20");

            assertThat(printedToOut()).isEqualTo("Good evening, Mary Ann!" + NEWLINE);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "hour {0}")
        @ValueSource(ints = {-1, 24, Integer.MIN_VALUE, Integer.MAX_VALUE})
        @DisplayName("rejects an hour outside 0 to 23, naming the hour in the message")
        void rejectsInvalidHour(int hour) {
            assertThatThrownBy(() -> Greeting.format("Minji", hour))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(hour));
        }

        @Test
        @DisplayName("main() throws NumberFormatException for an hour that isn't a number")
        void mainWithTextHour() {
            assertThatThrownBy(() -> runMain("Minji", "nine")).isInstanceOf(NumberFormatException.class);
            assertThat(printedToOut()).isEmpty();
        }

        @Test
        @DisplayName("main() lets the IllegalArgumentException for hour 24 through and prints nothing")
        void mainWithInvalidHour() {
            assertThatThrownBy(() -> runMain("Minji", "24")).isInstanceOf(IllegalArgumentException.class);
            assertThat(printedToOut()).isEmpty();
        }
    }
}
