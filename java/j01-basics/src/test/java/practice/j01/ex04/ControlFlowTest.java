package practice.j01.ex04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J01-04 · Control flow")
class ControlFlowTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @ParameterizedTest(name = "{0} is {1}", quoteTextArguments = false)
        @CsvSource({
                "101, INFORMATIONAL",
                "200, SUCCESS",
                "301, REDIRECTION",
                "404, CLIENT_ERROR",
                "503, SERVER_ERROR",
        })
        @DisplayName("statusClass() classifies common status codes")
        void commonStatusCodes(int code, StatusClass expected) {
            assertThat(ControlFlow.statusClass(code)).isEqualTo(expected);
        }

        @Test
        @DisplayName("openingHours() is 09:00-21:00 on Monday")
        void openOnMonday() {
            assertThat(ControlFlow.openingHours(DayOfWeek.MONDAY)).isEqualTo("09:00-21:00");
        }

        @Test
        @DisplayName("openingHours() is 10:00-18:00 on Saturday")
        void openOnSaturday() {
            assertThat(ControlFlow.openingHours(DayOfWeek.SATURDAY)).isEqualTo("10:00-18:00");
        }

        @Test
        @DisplayName("openingHours() is closed on Sunday")
        void closedOnSunday() {
            assertThat(ControlFlow.openingHours(DayOfWeek.SUNDAY)).isEqualTo("closed");
        }

        @Test
        @DisplayName("digitSum() adds the digits: 1234 gives 10")
        void digitSum() {
            assertThat(ControlFlow.digitSum(1234)).isEqualTo(10);
        }

        @Test
        @DisplayName("collatzSteps() needs 8 steps from 6")
        void collatzFromSix() {
            assertThat(ControlFlow.collatzSteps(6)).isEqualTo(8);
        }

        @Test
        @DisplayName("firstPair() finds the pair in the example grid")
        void firstPairExample() {
            int[][] grid = {
                    {1, 2, 3},
                    {4, 5, 5},
                    {7, 7, 8},
            };

            assertThat(ControlFlow.firstPair(grid)).containsExactly(1, 1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @ParameterizedTest(name = "{0} is {1}", quoteTextArguments = false)
        @CsvSource({
                "100, INFORMATIONAL",
                "199, INFORMATIONAL",
                "200, SUCCESS",
                "299, SUCCESS",
                "300, REDIRECTION",
                "399, REDIRECTION",
                "400, CLIENT_ERROR",
                "499, CLIENT_ERROR",
                "500, SERVER_ERROR",
                "599, SERVER_ERROR",
        })
        @DisplayName("statusClass() handles the first and last code of every class")
        void boundaries(int code, StatusClass expected) {
            assertThat(ControlFlow.statusClass(code)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = DayOfWeek.class, names = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"})
        @DisplayName("openingHours() is 09:00-21:00 on every weekday")
        void weekdays(DayOfWeek day) {
            assertThat(ControlFlow.openingHours(day)).isEqualTo("09:00-21:00");
        }

        @Test
        @DisplayName("digitSum() ignores the sign: -1234 gives 10")
        void digitSumNegative() {
            assertThat(ControlFlow.digitSum(-1234)).isEqualTo(10);
        }

        @ParameterizedTest(name = "{0} gives {1}", quoteTextArguments = false)
        @CsvSource({"0, 0", "7, 7", "-7, 7", "1000000, 1"})
        @DisplayName("digitSum() handles zero, single digits and zeros inside the number")
        void digitSumSmall(int number, int expected) {
            assertThat(ControlFlow.digitSum(number)).isEqualTo(expected);
        }

        @Test
        @DisplayName("digitSum() works for Integer.MAX_VALUE (2147483647 gives 46)")
        void digitSumMaxValue() {
            assertThat(ControlFlow.digitSum(Integer.MAX_VALUE)).isEqualTo(46);
        }

        @Test
        @DisplayName("digitSum() works for Integer.MIN_VALUE (-2147483648 gives 47)")
        void digitSumMinValue() {
            assertThat(ControlFlow.digitSum(Integer.MIN_VALUE)).isEqualTo(47);
        }

        @Test
        @DisplayName("collatzSteps() needs 0 steps from 1")
        void collatzFromOne() {
            assertThat(ControlFlow.collatzSteps(1)).isZero();
        }

        @Test
        @DisplayName("collatzSteps() needs 111 steps from 27")
        void collatzFromTwentySeven() {
            assertThat(ControlFlow.collatzSteps(27)).isEqualTo(111);
        }

        @Test
        @Timeout(value = 2, unit = TimeUnit.SECONDS)
        @DisplayName("collatzSteps() counts right when the numbers grow above Integer.MAX_VALUE (from 113383)")
        void collatzBeyondIntRange() {
            assertThat(ControlFlow.collatzSteps(113_383)).isEqualTo(247);
        }

        @Test
        @DisplayName("firstPair() returns the pair in the highest row when several rows have one")
        void firstPairEarliestRow() {
            int[][] grid = {
                    {1, 1, 2},
                    {3, 4, 5},
                    {6, 6, 6},
            };

            assertThat(ControlFlow.firstPair(grid)).containsExactly(0, 0);
        }

        @Test
        @DisplayName("firstPair() returns the leftmost pair when a row has two")
        void firstPairLeftmost() {
            int[][] grid = {{1, 2, 2, 3, 3}};

            assertThat(ControlFlow.firstPair(grid)).containsExactly(0, 1);
        }

        @Test
        @DisplayName("firstPair() finds a pair at the very end of the last row")
        void firstPairAtTheEnd() {
            int[][] grid = {
                    {1, 2},
                    {3, 4, 9, 9},
            };

            assertThat(ControlFlow.firstPair(grid)).containsExactly(1, 2);
        }

        @Test
        @DisplayName("firstPair() handles rows of different lengths, longest last")
        void firstPairJaggedRows() {
            int[][] grid = {
                    {1},
                    {2, 3},
                    {4, 5, 6, 6},
            };

            assertThat(ControlFlow.firstPair(grid)).containsExactly(2, 2);
        }

        @Test
        @DisplayName("firstPair() ignores equal cells across the end of a row")
        void firstPairNotAcrossRows() {
            int[][] grid = {
                    {1, 2},
                    {2, 3},
            };

            assertThat(ControlFlow.firstPair(grid)).isEmpty();
        }

        @Test
        @DisplayName("firstPair() ignores equal cells above each other")
        void firstPairNotVertical() {
            int[][] grid = {
                    {1, 2},
                    {1, 2},
            };

            assertThat(ControlFlow.firstPair(grid)).isEmpty();
        }

        @Test
        @DisplayName("firstPair() returns an empty array for a grid without rows")
        void firstPairNoRows() {
            assertThat(ControlFlow.firstPair(new int[0][])).isEmpty();
        }

        @Test
        @DisplayName("firstPair() returns an empty array when all rows are empty or have one cell")
        void firstPairShortRows() {
            int[][] grid = {{}, {7}, {}};

            assertThat(ControlFlow.firstPair(grid)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Randomized")
    class Randomized {

        @Test
        @DisplayName("digitSum() matches a simple model for 10,000 random ints")
        void digitSumMatchesModel() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            for (int step = 0; step < 10_000; step++) {
                int number = random.nextInt(); // any int, negative ones included

                // The model adds up the digit characters of the number's text, such as "-2147483648".
                int expected = String.valueOf(number).chars()
                        .filter(Character::isDigit)
                        .map(digit -> digit - '0')
                        .sum();

                assertThat(ControlFlow.digitSum(number))
                        .as("digitSum(%d) at step %d", number, step)
                        .isEqualTo(expected);
            }
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "code {0}")
        @ValueSource(ints = {0, 99, 600, 1000, -150, -404, Integer.MIN_VALUE, Integer.MAX_VALUE})
        @DisplayName("statusClass() rejects numbers that aren't status codes, naming the code")
        void rejectsInvalidCodes(int code) {
            assertThatThrownBy(() -> ControlFlow.statusClass(code))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(code));
        }

        @Test
        @DisplayName("openingHours() rejects a null day")
        void rejectsNullDay() {
            assertThatThrownBy(() -> ControlFlow.openingHours(null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "start {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("collatzSteps() rejects a start below 1")
        void rejectsInvalidStart(int start) {
            assertThatThrownBy(() -> ControlFlow.collatzSteps(start)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("firstPair() rejects a null grid")
        void rejectsNullGrid() {
            assertThatThrownBy(() -> ControlFlow.firstPair(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
