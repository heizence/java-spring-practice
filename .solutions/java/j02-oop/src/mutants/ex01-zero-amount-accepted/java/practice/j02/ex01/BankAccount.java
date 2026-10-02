package practice.j02.ex01;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

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

    // static: one counter for the whole class, shared by every account (and every thread). An AtomicLong rather
    // than a plain long, so that two threads opening accounts at the same moment can't get the same id (J08).
    private static final AtomicLong NEXT_ID = new AtomicLong(1);

    private final long id;
    private final String owner;
    private long balance; // the only field that changes, and only through the methods below

    /** Opens an account with a balance of 0. */
    public BankAccount(String owner) {
        this(owner, 0); // constructor chaining: one constructor holds all the rules
    }

    /** Opens an account with {@code openingBalance} won in it. */
    public BankAccount(String owner, long openingBalance) {
        Objects.requireNonNull(owner, "owner");
        if (owner.isBlank()) {
            throw new IllegalArgumentException("owner must not be blank");
        }
        if (openingBalance < 0) {
            throw new IllegalArgumentException("openingBalance must not be negative, but was " + openingBalance);
        }
        // Take the id only after every check has passed, so a failed call doesn't waste one.
        this.id = NEXT_ID.getAndIncrement();
        this.owner = owner;
        this.balance = openingBalance;
    }

    /** The id given to this account when it was opened. */
    public long id() {
        return id;
    }

    /** The owner's name, as passed to the constructor. */
    public String owner() {
        return owner;
    }

    /** The current balance in won. Never negative. */
    public long balance() {
        return balance;
    }

    /** Adds {@code amount} won to the balance. */
    public void deposit(long amount) {
        requirePositive(amount);
        // Math.addExact throws ArithmeticException instead of silently wrapping around to a negative number.
        balance = Math.addExact(balance, amount);
    }

    /** Takes {@code amount} won from the balance. */
    public void withdraw(long amount) {
        requirePositive(amount);
        requireFunds(amount);
        balance -= amount;
    }

    /** Moves {@code amount} won from this account to {@code target}. */
    public void transferTo(BankAccount target, long amount) {
        Objects.requireNonNull(target, "target");
        if (target == this) { // == on purpose: "the same object", not "an equal object"
            throw new IllegalArgumentException("cannot transfer from account " + id + " to itself");
        }
        requirePositive(amount);
        requireFunds(amount);
        // Compute the target's new balance before changing anything. If it overflows, addExact throws while both
        // accounts are still untouched. Withdrawing first and depositing second would lose the money.
        long newTargetBalance = Math.addExact(target.balance, amount);
        balance -= amount;
        target.balance = newTargetBalance; // private means "private to the class", so we may use target's field
    }

    @Override
    public String toString() {
        return "BankAccount[id=" + id + ", owner=" + owner + ", balance=" + balance + "]";
    }

    private static void requirePositive(long amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be greater than 0, but was " + amount);
        }
    }

    private void requireFunds(long amount) {
        if (amount > balance) {
            // IllegalStateException: the amount itself is fine, but the account's current state doesn't allow it.
            throw new IllegalStateException(
                    "account " + id + " has " + balance + " won, which is less than " + amount + " won");
        }
    }
}
