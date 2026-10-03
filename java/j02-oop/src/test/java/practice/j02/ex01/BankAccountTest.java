package practice.j02.ex01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J02-01 · Classes and encapsulation")
class BankAccountTest {

    private static BankAccount accountWith(long balance) {
        return new BankAccount("Mina Park", balance);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a new account has its owner and a balance of 0")
        void newAccount() {
            var account = new BankAccount("Mina Park");

            assertThat(account.owner()).isEqualTo("Mina Park");
            assertThat(account.balance()).isZero();
        }

        @Test
        @DisplayName("an account can open with a starting balance")
        void openingBalance() {
            var account = new BankAccount("Mina Park", 50_000);

            assertThat(account.balance()).isEqualTo(50_000);
        }

        @Test
        @DisplayName("deposit() adds to the balance")
        void depositAdds() {
            var account = accountWith(10_000);

            account.deposit(2_500);

            assertThat(account.balance()).isEqualTo(12_500);
        }

        @Test
        @DisplayName("withdraw() takes from the balance")
        void withdrawSubtracts() {
            var account = accountWith(10_000);

            account.withdraw(2_500);

            assertThat(account.balance()).isEqualTo(7_500);
        }

        @Test
        @DisplayName("transferTo() moves money from one account to another")
        void transferMovesMoney() {
            var from = accountWith(10_000);
            var to = new BankAccount("Joon Lee", 1_000);

            from.transferTo(to, 4_000);

            assertThat(from.balance()).isEqualTo(6_000);
            assertThat(to.balance()).isEqualTo(5_000);
        }

        @Test
        @DisplayName("toString() shows the id, the owner and the balance")
        void toStringShowsState() {
            var account = accountWith(15_000);

            assertThat(account).hasToString("BankAccount[id=" + account.id() + ", owner=Mina Park, balance=15000]");
        }

        @Test
        @DisplayName("each new account gets an id exactly one higher than the account before it")
        void idsCountUp() {
            var first = new BankAccount("Mina Park");
            var second = new BankAccount("Joon Lee");
            var third = new BankAccount("Seo-yeon Kim");

            assertThat(second.id()).isEqualTo(first.id() + 1);
            assertThat(third.id()).isEqualTo(second.id() + 1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("withdrawing the whole balance leaves exactly 0")
        void withdrawEverything() {
            var account = accountWith(10_000);

            account.withdraw(10_000);

            assertThat(account.balance()).isZero();
        }

        @Test
        @DisplayName("transferring the whole balance is allowed")
        void transferEverything() {
            var from = accountWith(10_000);
            var to = new BankAccount("Joon Lee");

            from.transferTo(to, 10_000);

            assertThat(from.balance()).isZero();
            assertThat(to.balance()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("an opening balance of 0 is allowed")
        void openingBalanceZero() {
            var account = new BankAccount("Mina Park", 0);

            assertThat(account.balance()).isZero();
        }

        @Test
        @DisplayName("a balance can grow up to exactly Long.MAX_VALUE")
        void balanceUpToMaxValue() {
            var account = accountWith(Long.MAX_VALUE - 10);

            account.deposit(10);

            assertThat(account.balance()).isEqualTo(Long.MAX_VALUE);
        }

        @Test
        @DisplayName("each account has its own balance")
        void accountsDontShareBalances() {
            var mina = new BankAccount("Mina Park", 10_000);
            var joon = new BankAccount("Joon Lee", 3_000);

            mina.deposit(500);

            assertThat(mina.balance()).isEqualTo(10_500);
            assertThat(joon.balance()).isEqualTo(3_000);
        }

        @Test
        @DisplayName("a constructor call that fails doesn't use up an id")
        void failedConstructorKeepsTheId() {
            var first = new BankAccount("Mina Park");

            assertThatThrownBy(() -> new BankAccount(" ")).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BankAccount("Joon Lee", -1)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new BankAccount(null)).isInstanceOf(NullPointerException.class);
            var second = new BankAccount("Joon Lee");

            assertThat(second.id()).isEqualTo(first.id() + 1);
        }

        @Test
        @DisplayName("keeps every field private")
        void everyFieldIsPrivate() {
            var fields = BankAccount.class.getDeclaredFields();

            assertThat(fields).allSatisfy(field -> assertThat(Modifier.isPrivate(field.getModifiers()))
                    .as("field '%s' is private", field.getName())
                    .isTrue());
        }

        @Test
        @DisplayName("has no setters")
        void noSetters() {
            var methods = BankAccount.class.getMethods();

            assertThat(methods).extracting(Method::getName).noneMatch(name -> name.startsWith("set"));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a null owner")
        void nullOwner() {
            assertThatThrownBy(() -> new BankAccount(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new BankAccount(null, 100)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "owner {0}")
        @ValueSource(strings = {"", " ", "\t\n"})
        @DisplayName("rejects a blank owner")
        void blankOwner(String owner) {
            assertThatThrownBy(() -> new BankAccount(owner)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "opening balance {0}")
        @ValueSource(longs = {-1, Long.MIN_VALUE})
        @DisplayName("rejects a negative opening balance")
        void negativeOpeningBalance(long openingBalance) {
            assertThatThrownBy(() -> new BankAccount("Mina Park", openingBalance))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(openingBalance));
        }

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        @DisplayName("rejects a deposit of 0 or less and keeps the balance")
        void depositNotPositive(long amount) {
            var account = accountWith(10_000);

            assertThatThrownBy(() -> account.deposit(amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(amount));
            assertThat(account.balance()).isEqualTo(10_000);
        }

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        @DisplayName("rejects a withdrawal of 0 or less and keeps the balance")
        void withdrawNotPositive(long amount) {
            var account = accountWith(10_000);

            assertThatThrownBy(() -> account.withdraw(amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(amount));
            assertThat(account.balance()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("refuses to withdraw more than the balance and keeps the balance")
        void withdrawTooMuch() {
            var account = accountWith(10_000);

            assertThatThrownBy(() -> account.withdraw(10_001)).isInstanceOf(IllegalStateException.class);
            assertThat(account.balance()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("refuses any withdrawal from an empty account")
        void withdrawFromEmptyAccount() {
            var account = new BankAccount("Mina Park");

            assertThatThrownBy(() -> account.withdraw(1)).isInstanceOf(IllegalStateException.class);
            assertThat(account.balance()).isZero();
        }

        @Test
        @DisplayName("rejects a deposit that would go past Long.MAX_VALUE and keeps the balance")
        void depositOverflow() {
            var account = accountWith(Long.MAX_VALUE - 10);

            assertThatThrownBy(() -> account.deposit(11)).isInstanceOf(ArithmeticException.class);
            assertThat(account.balance()).isEqualTo(Long.MAX_VALUE - 10);
        }

        @Test
        @DisplayName("rejects a transfer to null and keeps the balance")
        void transferToNull() {
            var account = accountWith(10_000);

            assertThatThrownBy(() -> account.transferTo(null, 1_000)).isInstanceOf(NullPointerException.class);
            assertThat(account.balance()).isEqualTo(10_000);
        }

        @Test
        @DisplayName("rejects a transfer to the same account and keeps the balance")
        void transferToItself() {
            var account = accountWith(10_000);

            assertThatThrownBy(() -> account.transferTo(account, 1_000)).isInstanceOf(IllegalArgumentException.class);
            assertThat(account.balance()).isEqualTo(10_000);
        }

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        @DisplayName("rejects a transfer of 0 or less and changes neither account")
        void transferNotPositive(long amount) {
            var from = accountWith(10_000);
            var to = new BankAccount("Joon Lee", 1_000);

            assertThatThrownBy(() -> from.transferTo(to, amount))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(String.valueOf(amount));
            assertThat(from.balance()).isEqualTo(10_000);
            assertThat(to.balance()).isEqualTo(1_000);
        }

        @Test
        @DisplayName("refuses to transfer more than the balance and changes neither account")
        void transferTooMuch() {
            var from = accountWith(10_000);
            var to = new BankAccount("Joon Lee", 1_000);

            assertThatThrownBy(() -> from.transferTo(to, 10_001)).isInstanceOf(IllegalStateException.class);
            assertThat(from.balance()).isEqualTo(10_000);
            assertThat(to.balance()).isEqualTo(1_000);
        }

        @Test
        @DisplayName("rejects a transfer that would push the target past Long.MAX_VALUE and changes neither account")
        void transferOverflow() {
            var from = accountWith(10_000);
            var to = new BankAccount("Joon Lee", Long.MAX_VALUE - 5);

            assertThatThrownBy(() -> from.transferTo(to, 6)).isInstanceOf(ArithmeticException.class);
            assertThat(from.balance()).isEqualTo(10_000);
            assertThat(to.balance()).isEqualTo(Long.MAX_VALUE - 5);
        }
    }
}
