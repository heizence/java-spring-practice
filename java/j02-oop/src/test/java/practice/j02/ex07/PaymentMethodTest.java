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
class PaymentMethodTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a Card keeps its last four digits and whether it is international")
        void card() {
            var card = new Card("4242", true);

            assertThat(card.last4()).isEqualTo("4242");
            assertThat(card.international()).isTrue();
        }

        @Test
        @DisplayName("a BankTransfer keeps its bank code and account number")
        void bankTransfer() {
            var transfer = new BankTransfer("004", "1234567890");

            assertThat(transfer.bankCode()).isEqualTo("004");
            assertThat(transfer.accountNumber()).isEqualTo("1234567890");
        }

        @Test
        @DisplayName("Points keep their balance")
        void points() {
            var points = new Points(15_000);

            assertThat(points.balance()).isEqualTo(15_000);
        }

        @Test
        @DisplayName("records with the same values are equal and have the same hash code")
        void recordEquality() {
            var first = new Card("4242", false);
            var second = new Card("4242", false);

            assertThat(first).isEqualTo(second);
            assertThat(first.hashCode()).isEqualTo(second.hashCode());
            assertThat(first).isNotEqualTo(new Card("4242", true));
        }

        @Test
        @DisplayName("records have a readable toString()")
        void recordToString() {
            assertThat(new Card("4242", false)).hasToString("Card[last4=4242, international=false]");
            assertThat(new Points(100)).hasToString("Points[balance=100]");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("BankTransfer removes the hyphens from the account number")
        void removesHyphens() {
            var transfer = new BankTransfer("088", "123-456-789012");

            assertThat(transfer.accountNumber()).isEqualTo("123456789012");
            assertThat(transfer).isEqualTo(new BankTransfer("088", "123456789012"));
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"1234567890", "12345678901234", "1234-5678-90", "-1234567890-"})
        @DisplayName("BankTransfer accepts 10 to 14 digits")
        void accountNumberBoundaries(String accountNumber) {
            var transfer = new BankTransfer("004", accountNumber);

            assertThat(transfer.accountNumber()).isEqualTo(accountNumber.replace("-", ""));
        }

        @Test
        @DisplayName("a balance of 0 points is allowed")
        void zeroPoints() {
            assertThat(new Points(0).balance()).isZero();
        }

        @Test
        @DisplayName("a card number may start with 0")
        void leadingZero() {
            assertThat(new Card("0007", false).last4()).isEqualTo("0007");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", "123", "12345", "12a4", " 4242", "4242 ", "４２４２"})
        @DisplayName("Card rejects last4 unless it is exactly 4 ASCII digits")
        void invalidLast4(String last4) {
            assertThatThrownBy(() -> new Card(last4, false))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(last4);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", "04", "0044", "0a4", "0-4"})
        @DisplayName("BankTransfer rejects a bank code unless it is exactly 3 ASCII digits")
        void invalidBankCode(String bankCode) {
            assertThatThrownBy(() -> new BankTransfer(bankCode, "1234567890"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(bankCode);
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", "----------", "123456789", "123456789012345", "12345-6789", "123456789O"})
        @DisplayName("BankTransfer rejects an account number without 10 to 14 digits")
        void invalidAccountNumber(String accountNumber) {
            assertThatThrownBy(() -> new BankTransfer("004", accountNumber))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("BankTransfer puts a rejected account number in the exception message")
        void accountNumberInMessage() {
            assertThatThrownBy(() -> new BankTransfer("004", "123456789"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("123456789");
        }

        @ParameterizedTest(name = "balance {0}")
        @ValueSource(longs = {-1, Long.MIN_VALUE})
        @DisplayName("Points rejects a negative balance")
        void negativePoints(long balance) {
            assertThatThrownBy(() -> new Points(balance))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(balance));
        }

        @Test
        @DisplayName("rejects null components")
        void nullComponents() {
            assertThatThrownBy(() -> new Card(null, false)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BankTransfer(null, "1234567890")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BankTransfer("004", null)).isInstanceOf(NullPointerException.class);
        }
    }
}
