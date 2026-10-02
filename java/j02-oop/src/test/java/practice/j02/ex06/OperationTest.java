package practice.j02.ex06;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J02-06 · Enums with behavior")
class OperationTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @ParameterizedTest(name = "{0} has symbol {1}", quoteTextArguments = false)
        @CsvSource({"PLUS, +", "MINUS, -", "TIMES, *", "DIVIDE, /"})
        @DisplayName("each operation has its symbol")
        void symbols(Operation operation, String symbol) {
            assertThat(operation.symbol()).isEqualTo(symbol);
        }

        @ParameterizedTest(name = "7 {0} 3 = {1}", quoteTextArguments = false)
        @CsvSource({"PLUS, 10", "MINUS, 4", "TIMES, 21", "DIVIDE, 2"})
        @DisplayName("apply() computes the result")
        void apply(Operation operation, long expected) {
            assertThat(operation.apply(7, 3)).isEqualTo(expected);
        }

        @ParameterizedTest(name = "\"{0}\" is {1}", quoteTextArguments = false)
        @CsvSource({"+, PLUS", "-, MINUS", "*, TIMES", "/, DIVIDE"})
        @DisplayName("fromSymbol() finds the operation for a symbol")
        void fromSymbol(String symbol, Operation expected) {
            assertThat(Operation.fromSymbol(symbol)).isEqualTo(expected);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("division rounds toward zero, like the / operator")
        void divisionRoundsTowardZero() {
            assertThat(Operation.DIVIDE.apply(-7, 2)).isEqualTo(-3);
            assertThat(Operation.DIVIDE.apply(7, -2)).isEqualTo(-3);
            assertThat(Operation.DIVIDE.apply(1, 3)).isZero();
        }

        @Test
        @DisplayName("works with negative numbers")
        void negativeNumbers() {
            assertThat(Operation.MINUS.apply(-5, -7)).isEqualTo(2);
            assertThat(Operation.TIMES.apply(-4, 5)).isEqualTo(-20);
            assertThat(Operation.PLUS.apply(-4, 4)).isZero();
        }

        @Test
        @DisplayName("results right at the edges of the long range are fine")
        void edgesOfLongRange() {
            assertThat(Operation.PLUS.apply(Long.MAX_VALUE - 1, 1)).isEqualTo(Long.MAX_VALUE);
            assertThat(Operation.MINUS.apply(Long.MIN_VALUE + 1, 1)).isEqualTo(Long.MIN_VALUE);
            assertThat(Operation.TIMES.apply(Long.MIN_VALUE, 1)).isEqualTo(Long.MIN_VALUE);
            assertThat(Operation.DIVIDE.apply(Long.MIN_VALUE, 1)).isEqualTo(Long.MIN_VALUE);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(Operation.class)
        @DisplayName("fromSymbol() finds every operation by its own symbol")
        void roundTrip(Operation operation) {
            assertThat(Operation.fromSymbol(operation.symbol())).isSameAs(operation);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"%", "", " +", "+ ", "plus", "PLUS", "÷", "**"})
        @DisplayName("fromSymbol() rejects an unknown symbol and names it in the message")
        void unknownSymbol(String symbol) {
            assertThatThrownBy(() -> Operation.fromSymbol(symbol))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(symbol);
        }

        @Test
        @DisplayName("fromSymbol(null) throws NullPointerException")
        void nullSymbol() {
            assertThatThrownBy(() -> Operation.fromSymbol(null)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "{0}({1}, {2})", quoteTextArguments = false)
        @CsvSource({
            "PLUS, 9223372036854775807, 1",
            "MINUS, -9223372036854775808, 1",
            "TIMES, 9223372036854775807, 2",
            "DIVIDE, -9223372036854775808, -1"
        })
        @DisplayName("throws ArithmeticException on overflow instead of wrapping around")
        void overflow(Operation operation, long left, long right) {
            assertThatThrownBy(() -> operation.apply(left, right)).isInstanceOf(ArithmeticException.class);
        }

        @Test
        @DisplayName("dividing by 0 throws ArithmeticException")
        void divisionByZero() {
            assertThatThrownBy(() -> Operation.DIVIDE.apply(7, 0)).isInstanceOf(ArithmeticException.class);
        }
    }
}
