package com.example.bookstore.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Template · Wallet")
class WalletTest {

    private final Wallet wallet = Wallet.open(Member.register("minji@example.com", "Minji Kim", "{noop}secret"));

    @Test
    @DisplayName("a new wallet is empty")
    void startsEmpty() {
        assertThat(wallet.getBalance()).isZero();
    }

    @Nested
    @DisplayName("deposit")
    class Deposit {

        @Test
        @DisplayName("adds money and returns the transaction that records it")
        void addsMoney() {
            WalletTransaction transaction = wallet.deposit(50_000, WalletTransactionType.CHARGE);

            assertThat(wallet.getBalance()).isEqualTo(50_000);
            assertThat(transaction.getWallet()).isSameAs(wallet);
            assertThat(transaction.getType()).isEqualTo(WalletTransactionType.CHARGE);
            assertThat(transaction.getAmount()).isEqualTo(50_000);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = WalletTransactionType.class, names = {"PAYMENT", "TRANSFER_OUT"})
        @DisplayName("rejects a type that takes money out")
        void rejectsWithdrawalTypes(WalletTransactionType type) {
            assertThatThrownBy(() -> wallet.deposit(1_000, type)).isInstanceOf(IllegalArgumentException.class);
            assertThat(wallet.getBalance()).isZero();
        }

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -1})
        @DisplayName("rejects an amount below 1")
        void rejectsNonPositive(long amount) {
            assertThatThrownBy(() -> wallet.deposit(amount, WalletTransactionType.CHARGE))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("withdraw")
    class Withdraw {

        @Test
        @DisplayName("takes money out and returns the transaction that records it")
        void takesMoney() {
            wallet.deposit(50_000, WalletTransactionType.CHARGE);

            WalletTransaction transaction = wallet.withdraw(36_000, WalletTransactionType.PAYMENT);

            assertThat(wallet.getBalance()).isEqualTo(14_000);
            assertThat(transaction.getType()).isEqualTo(WalletTransactionType.PAYMENT);
            assertThat(transaction.getAmount()).isEqualTo(36_000);
        }

        @Test
        @DisplayName("can take the whole balance")
        void takesTheWholeBalance() {
            wallet.deposit(36_000, WalletTransactionType.CHARGE);

            wallet.withdraw(36_000, WalletTransactionType.PAYMENT);

            assertThat(wallet.getBalance()).isZero();
        }

        @Test
        @DisplayName("rejects more than the balance with INSUFFICIENT_BALANCE, and keeps the balance")
        void rejectsMoreThanTheBalance() {
            wallet.deposit(36_000, WalletTransactionType.CHARGE);

            assertThatThrownBy(() -> wallet.withdraw(36_001, WalletTransactionType.PAYMENT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("The wallet has 36000 won, but 36001 won are needed.")
                    .extracting("errorCode").isEqualTo(ErrorCode.INSUFFICIENT_BALANCE);
            assertThat(wallet.getBalance()).isEqualTo(36_000);
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = WalletTransactionType.class, names = {"CHARGE", "REFUND", "TRANSFER_IN"})
        @DisplayName("rejects a type that adds money")
        void rejectsDepositTypes(WalletTransactionType type) {
            wallet.deposit(10_000, WalletTransactionType.CHARGE);

            assertThatThrownBy(() -> wallet.withdraw(1_000, type)).isInstanceOf(IllegalArgumentException.class);
            assertThat(wallet.getBalance()).isEqualTo(10_000);
        }

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -1})
        @DisplayName("rejects an amount below 1")
        void rejectsNonPositive(long amount) {
            assertThatThrownBy(() -> wallet.withdraw(amount, WalletTransactionType.PAYMENT))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
