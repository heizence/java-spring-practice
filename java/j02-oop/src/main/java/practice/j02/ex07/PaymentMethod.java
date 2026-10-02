package practice.j02.ex07;

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
            // TODO: J02-07
            throw new UnsupportedOperationException("TODO: J02-07");
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
            // TODO: J02-07
            throw new UnsupportedOperationException("TODO: J02-07");
        }
    }

    /**
     * The shop's reward points; one point is worth one won.
     *
     * @param balance the points the customer has, never negative
     */
    record Points(long balance) implements PaymentMethod {

        public Points {
            // TODO: J02-07
            throw new UnsupportedOperationException("TODO: J02-07");
        }
    }
}
