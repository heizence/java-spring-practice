package practice.j08.ex04;

import java.util.concurrent.locks.ReentrantLock;

/**
 * A bank account. Amounts are whole numbers (cents), so no rounding is involved.
 *
 * <p><strong>This class is correct.</strong> The bugs of J08-04 are in {@link Bank}.
 *
 * <p>The balance is <em>guarded by</em> {@link #lock()}: code may read or change it only while it holds that lock.
 * {@link #balance()} takes the lock by itself. {@link #withdraw} and {@link #deposit} don't, because a transfer has
 * to hold the locks of <em>both</em> accounts for its whole check-and-change sequence. Their callers must hold the
 * lock already.
 */
public final class Account {

    private final long id;
    private final ReentrantLock lock = new ReentrantLock();
    private long balance; // guarded by lock

    /**
     * Creates an account. Ids must be unique: {@link Bank} uses them to decide in which order to lock accounts.
     *
     * @throws IllegalArgumentException if {@code initialBalance} is negative
     */
    public Account(long id, long initialBalance) {
        if (initialBalance < 0) {
            throw new IllegalArgumentException("initialBalance must not be negative, but was " + initialBalance);
        }
        this.id = id;
        this.balance = initialBalance;
    }

    /** The unique id of this account. */
    public long id() {
        return id;
    }

    /** The current balance. Takes the lock while it reads. */
    public long balance() {
        lock.lock();
        try {
            return balance;
        } finally {
            lock.unlock();
        }
    }

    /** The lock that guards the balance. */
    ReentrantLock lock() {
        return lock;
    }

    /** Takes money out. The caller must hold {@link #lock()} and must have checked the balance. */
    void withdraw(long amount) {
        balance -= amount;
    }

    /** Puts money in. The caller must hold {@link #lock()}. */
    void deposit(long amount) {
        balance += amount;
    }

    @Override
    public String toString() {
        return "Account " + id;
    }
}
