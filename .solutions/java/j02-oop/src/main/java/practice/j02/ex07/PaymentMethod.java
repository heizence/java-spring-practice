package practice.j02.ex07;

import java.util.Objects;

/**
 * How a customer pays.
 *
 * <p>The interface is {@code sealed}: the three records below are the only kinds of payment method. So a
 * {@code switch} over a {@code PaymentMethod} can list every kind and needs no {@code default} branch. (The
 * records are declared in this file, so the interface doesn't need a {@code permits} clause.)
 *
 * <p>Every record checks its values in a compact constructor. {@code null} components throw
 * {@link NullPointerException}; other bad values throw {@link IllegalArgumentException} with the value in the
 * message.
 */
public sealed interface PaymentMethod {

    /**
     * A credit or debit card, identified by the last four digits of its number.
     *
     * @param last4         exactly 4 ASCII digits, such as {@code "4242"}
     * @param international {@code true} for a card issued outside Korea
     */
    record Card(String last4, boolean international) implements PaymentMethod {

        public Card {
            // A compact constructor has no parameter list. It runs before the fields are assigned, so it is the
            // place to validate. (A record nested in an interface is public, so its constructor must be too.)
            Objects.requireNonNull(last4, "last4");
            if (!last4.matches("[0-9]{4}")) {
                throw new IllegalArgumentException("last4 must be exactly 4 digits, but was \"" + last4 + "\"");
            }
        }
    }

    /**
     * A bank transfer.
     *
     * @param bankCode      exactly 3 ASCII digits, such as {@code "004"}
     * @param accountNumber 10 to 14 ASCII digits after every hyphen is removed: {@code "123-456-789012"} is stored
     *                      as {@code "123456789012"}
     */
    record BankTransfer(String bankCode, String accountNumber) implements PaymentMethod {

        public BankTransfer {
            Objects.requireNonNull(bankCode, "bankCode");
            Objects.requireNonNull(accountNumber, "accountNumber");
            if (!bankCode.matches("[0-9]{3}")) {
                throw new IllegalArgumentException("bankCode must be exactly 3 digits, but was \"" + bankCode + "\"");
            }
            // A compact constructor may assign its parameters. The record stores whatever they hold at the end,
            // so this is how a record normalizes its values.
            String digits = accountNumber.replace("-", "");
            if (!digits.matches("[0-9]{10,14}")) {
                throw new IllegalArgumentException(
                        "accountNumber must have 10 to 14 digits, but was \"" + accountNumber + "\"");
            }
            accountNumber = digits;
        }
    }

    /**
     * The shop's reward points; one point is worth one won.
     *
     * @param balance the points the customer has, never negative
     */
    record Points(long balance) implements PaymentMethod {

        public Points {
            if (balance < 0) {
                throw new IllegalArgumentException("balance must not be negative, but was " + balance);
            }
        }
    }
}
