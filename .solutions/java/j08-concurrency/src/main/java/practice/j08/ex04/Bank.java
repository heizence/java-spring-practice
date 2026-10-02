package practice.j08.ex04;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Moves money between accounts. One {@code Bank} serves all request threads at the same time.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A transfer takes {@code amount} from {@code from} and adds it to {@code to} as one step: no other thread
 *       ever sees the money missing from both accounts, or in both.</li>
 *   <li>Money is never created or lost, however many transfers run at the same time, and no balance ever goes
 *       below 0. If {@code from} has less than {@code amount}, the transfer throws
 *       {@link InsufficientFundsException} and changes nothing.</li>
 *   <li>An account's balance is guarded by {@link Account#lock()}. Both methods hold the locks of both accounts
 *       while they check and change the balances.</li>
 *   <li>Transfers never deadlock, even when two threads transfer between the same two accounts in opposite
 *       directions. Both methods lock the two accounts in the same order: lower id first.</li>
 *   <li>{@link #tryTransfer} waits for each lock until it is free or {@code timeout} has passed. If it can't get
 *       both locks in time, it returns {@code false}, changes nothing and holds no lock afterwards.</li>
 *   <li>{@code amount} must be positive, {@code from} and {@code to} must be different accounts, and
 *       {@code timeout} must not be negative. Otherwise the methods throw {@link IllegalArgumentException}.
 *       {@code null} arguments throw {@link NullPointerException}.</li>
 * </ul>
 */
public final class Bank {

    /** Moves {@code amount} from {@code from} to {@code to}, waiting as long as needed for the accounts. */
    public void transfer(Account from, Account to, long amount) {
        checkArguments(from, to, amount);
        // Bug 1 was the lock order. With "from first", transfer(a, b) locks a and waits for b while transfer(b, a)
        // locks b and waits for a: a deadlock. With "lower id first", both lock the same account first, so one of
        // them waits until the other is done.
        Account first = from.id() < to.id() ? from : to;
        Account second = first == from ? to : from;
        // Bug 2 was the kind of lock. synchronized (from) takes the object's built-in monitor, which has nothing to
        // do with from.lock(). So transfer and tryTransfer didn't exclude each other, and could change the same
        // balance at the same moment. Code that protects the same data must use the same lock.
        first.lock().lock();
        try {
            second.lock().lock();
            try {
                move(from, to, amount);
            } finally {
                second.lock().unlock();
            }
        } finally {
            first.lock().unlock();
        }
    }

    /**
     * Moves {@code amount} like {@link #transfer}, but waits at most {@code timeout} for each account.
     *
     * @return {@code true} if the money was moved, {@code false} if an account stayed busy for too long
     * @throws InterruptedException if the calling thread is interrupted while it waits for an account
     */
    public boolean tryTransfer(Account from, Account to, long amount, Duration timeout) throws InterruptedException {
        checkArguments(from, to, amount);
        long waitNanos = checkTimeout(timeout).toNanos();
        Account first = from.id() < to.id() ? from : to;
        Account second = first == from ? to : from;

        if (!first.lock().tryLock(waitNanos, TimeUnit.NANOSECONDS)) {
            return false;
        }
        // Bug 3: from here on we hold `first`, and every way out of the method must release it, including the early
        // "return false" below. The outer finally block does that. Before, a failed second tryLock returned with
        // `first` still locked, and that account stayed blocked for every other thread forever.
        try {
            if (!second.lock().tryLock(waitNanos, TimeUnit.NANOSECONDS)) {
                return false;
            }
            try {
                move(from, to, amount);
                return true;
            } finally {
                second.lock().unlock();
            }
        } finally {
            first.lock().unlock();
        }
    }

    /** Checks the balance and moves the money. The caller must hold the locks of both accounts. */
    private static void move(Account from, Account to, long amount) {
        long balance = from.balance();
        if (balance < amount) {
            throw new InsufficientFundsException(from, balance, amount);
        }
        from.withdraw(amount);
        to.deposit(amount);
    }

    private static void checkArguments(Account from, Account to, long amount) {
        Objects.requireNonNull(from, "from");
        Objects.requireNonNull(to, "to");
        if (from.id() == to.id()) {
            throw new IllegalArgumentException("from and to must be different accounts, but both are " + from);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive, but was " + amount);
        }
    }

    private static Duration checkTimeout(Duration timeout) {
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative, but was " + timeout);
        }
        return timeout;
    }
}
