package practice.j02.ex01;

/**
 * A bank account that holds a balance in Korean won.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Every new account gets the next id: its id is exactly one more than the id of the account created
 *       before it. A constructor call that fails doesn't use up an id.</li>
 *   <li>The owner is required. {@code null} throws {@link NullPointerException}; an empty name or a name made only
 *       of whitespace throws {@link IllegalArgumentException}.</li>
 *   <li>The opening balance must not be negative: {@link IllegalArgumentException} with the value in its
 *       message.</li>
 *   <li>Every amount passed to {@link #deposit}, {@link #withdraw} and {@link #transferTo} must be greater than 0.
 *       Otherwise the method throws {@link IllegalArgumentException} with the amount in its message.</li>
 *   <li>The balance never goes below 0: withdrawing or transferring more than the balance throws
 *       {@link IllegalStateException}.</li>
 *   <li>A deposit or transfer that would push a balance above {@link Long#MAX_VALUE} throws
 *       {@link ArithmeticException}.</li>
 *   <li>Transferring to the same account throws {@link IllegalArgumentException}; a {@code null} target throws
 *       {@link NullPointerException}.</li>
 *   <li>A method that throws changes nothing. In particular, a failed transfer changes neither account.</li>
 *   <li>Every field is private and there are no setters: the balance changes only through the methods above.</li>
 *   <li>{@link #toString()} looks like {@code BankAccount[id=7, owner=Mina Park, balance=15000]}.</li>
 * </ul>
 */
public final class BankAccount {

    /** Opens an account with a balance of 0. */
    public BankAccount(String owner) {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }

    /** Opens an account with {@code openingBalance} won in it. */
    public BankAccount(String owner, long openingBalance) {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }

    /** The id given to this account when it was opened. */
    public long id() {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }

    /** The owner's name, as passed to the constructor. */
    public String owner() {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }

    /** The current balance in won. Never negative. */
    public long balance() {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }

    /** Adds {@code amount} won to the balance. */
    public void deposit(long amount) {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }

    /** Takes {@code amount} won from the balance. */
    public void withdraw(long amount) {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }

    /** Moves {@code amount} won from this account to {@code target}. */
    public void transferTo(BankAccount target, long amount) {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }

    @Override
    public String toString() {
        // TODO: J02-01
        throw new UnsupportedOperationException("TODO: J02-01");
    }
}
