package practice.j02.ex07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import practice.j02.ex07.PaymentMethod.BankTransfer;
import practice.j02.ex07.PaymentMethod.Card;
import practice.j02.ex07.PaymentMethod.Points;

@DisplayName("J02-07 · Records, sealed types and pattern matching")
class FeeCalculatorTest {

    // Methods rather than constants: while the records are unfinished, a constant would stop the whole class.
    private static PaymentMethod aDomesticCard() {
        return new Card("4242", false);
    }

    private static PaymentMethod anInternationalCard() {
        return new Card("1881", true);
    }

    private static PaymentMethod aTransfer() {
        return new BankTransfer("004", "123-456-789012");
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a domestic card costs 2% of the amount")
        void domesticCard() {
            assertThat(FeeCalculator.fee(aDomesticCard(), 10_000)).isEqualTo(200);
        }

        @Test
        @DisplayName("an international card costs 3% of the amount")
        void internationalCard() {
            assertThat(FeeCalculator.fee(anInternationalCard(), 10_000)).isEqualTo(300);
        }

        @Test
        @DisplayName("a bank transfer costs 500 won")
        void bankTransfer() {
            assertThat(FeeCalculator.fee(aTransfer(), 50_000)).isEqualTo(500);
        }

        @Test
        @DisplayName("paying with points costs nothing")
        void points() {
            assertThat(FeeCalculator.fee(new Points(20_000), 10_000)).isZero();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("card fees are rounded down to whole won")
        void roundsDown() {
            assertThat(FeeCalculator.fee(aDomesticCard(), 1_234)).isEqualTo(24); // 24.68
            assertThat(FeeCalculator.fee(anInternationalCard(), 1_234)).isEqualTo(37); // 37.02
            assertThat(FeeCalculator.fee(aDomesticCard(), 49)).isZero(); // 0.98
        }

        @Test
        @DisplayName("a bank transfer of exactly 100,000 won is free")
        void freeTransferBoundary() {
            assertThat(FeeCalculator.fee(aTransfer(), 100_000)).isZero();
        }

        @Test
        @DisplayName("a bank transfer of 99,999 won still costs 500 won")
        void justBelowFreeTransfer() {
            assertThat(FeeCalculator.fee(aTransfer(), 99_999)).isEqualTo(500);
        }

        @Test
        @DisplayName("the smallest amount, 1 won, is allowed")
        void smallestAmount() {
            assertThat(FeeCalculator.fee(aDomesticCard(), 1)).isZero();
            assertThat(FeeCalculator.fee(aTransfer(), 1)).isEqualTo(500);
            assertThat(FeeCalculator.fee(new Points(1), 1)).isZero();
        }

        @Test
        @DisplayName("points that exactly cover the amount are enough")
        void pointsExactlyEnough() {
            assertThat(FeeCalculator.fee(new Points(10_000), 10_000)).isZero();
        }

        @Test
        @DisplayName("works for a large amount that doesn't overflow")
        void largeAmount() {
            assertThat(FeeCalculator.fee(anInternationalCard(), 1_000_000_000_000L)).isEqualTo(30_000_000_000L);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        @DisplayName("rejects an amount below 1 won")
        void amountTooSmall(long amount) {
            assertThatThrownBy(() -> FeeCalculator.fee(aDomesticCard(), amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(amount));
            assertThatThrownBy(() -> FeeCalculator.fee(new Points(100), amount))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a null payment method")
        void nullMethod() {
            assertThatThrownBy(() -> FeeCalculator.fee(null, 10_000)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects points that don't cover the amount, naming both numbers")
        void notEnoughPoints() {
            assertThatThrownBy(() -> FeeCalculator.fee(new Points(9_999), 10_000))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("9999")
                    .hasMessageContaining("10000");
        }

        @Test
        @DisplayName("throws ArithmeticException when the fee computation overflows")
        void overflow() {
            assertThatThrownBy(() -> FeeCalculator.fee(aDomesticCard(), Long.MAX_VALUE))
                    .isInstanceOf(ArithmeticException.class);
        }
    }
}
