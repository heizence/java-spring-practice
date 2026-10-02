package practice.j08.ex04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.LongStream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import practice.j08.support.Blocking;
import practice.testkit.Concurrently;

@DisplayName("J08-04 · Locks and deadlocks")
class BankTest {

    private static final int THREADS = 8;
    /** Far more than the threads need; a run that takes longer is stuck in a deadlock. */
    private static final Duration DEADLOCK_TIMEOUT = Duration.ofSeconds(5);

    private final Bank bank = new Bank();

    /** {@link Bank#tryTransfer} with a 1-second timeout, without the checked exception. */
    private boolean tryTransfer(Account from, Account to, long amount) {
        try {
            return bank.tryTransfer(from, to, amount, Duration.ofSeconds(1));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /**
     * Holds an account's lock on another thread, the way a slow transfer would. It lets go when it is closed, or after
     * {@code maxHold} at the latest, so a test can never leave an account locked.
     */
    private static final class LockHolder implements AutoCloseable {

        private final CountDownLatch release = new CountDownLatch(1);
        private final Thread thread;

        LockHolder(Account account, Duration maxHold) {
            var locked = new CountDownLatch(1);
            thread = Thread.ofPlatform().daemon().name("lock-holder").start(() -> {
                account.lock().lock();
                try {
                    locked.countDown();
                    release.await(maxHold.toMillis(), TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    account.lock().unlock();
                }
            });
            Blocking.await(locked);
        }

        @Override
        public void close() throws InterruptedException {
            release.countDown();
            thread.join(Blocking.PATIENCE);
        }
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("transfer() moves the amount from one account to the other")
        void transferMovesMoney() {
            var from = new Account(1, 100);
            var to = new Account(2, 50);

            bank.transfer(from, to, 30);

            assertThat(from.balance()).isEqualTo(70);
            assertThat(to.balance()).isEqualTo(80);
        }

        @Test
        @DisplayName("transfer() works in both directions")
        void transferBothDirections() {
            var a = new Account(1, 100);
            var b = new Account(2, 100);

            bank.transfer(a, b, 30);
            bank.transfer(b, a, 10);

            assertThat(a.balance()).isEqualTo(80);
            assertThat(b.balance()).isEqualTo(120);
        }

        @Test
        @DisplayName("tryTransfer() moves the amount and returns true when no other thread holds the accounts")
        void tryTransferMovesMoney() throws InterruptedException {
            var from = new Account(1, 100);
            var to = new Account(2, 50);

            boolean moved = bank.tryTransfer(from, to, 30, Duration.ofSeconds(1));

            assertThat(moved).isTrue();
            assertThat(from.balance()).isEqualTo(70);
            assertThat(to.balance()).isEqualTo(80);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("transfer() can move the whole balance")
        void transferWholeBalance() {
            var from = new Account(1, 100);
            var to = new Account(2, 0);

            bank.transfer(from, to, 100);

            assertThat(from.balance()).isZero();
            assertThat(to.balance()).isEqualTo(100);
        }

        @Test
        @DisplayName("tryTransfer() returns false when another thread holds an account until the timeout ends")
        void tryTransferGivesUp() throws InterruptedException {
            var from = new Account(1, 100);
            var to = new Account(2, 100);

            boolean moved;
            try (var holder = new LockHolder(to, Duration.ofSeconds(5))) {
                moved = bank.tryTransfer(from, to, 30, Duration.ofMillis(100));
            }

            assertThat(moved).isFalse();
            assertThat(from.balance()).isEqualTo(100);
            assertThat(to.balance()).isEqualTo(100);
        }

        @Test
        @DisplayName("tryTransfer() holds no lock after it gives up")
        void tryTransferReleasesLocksWhenGivingUp() throws InterruptedException {
            var from = new Account(1, 100);
            var to = new Account(2, 100);

            try (var holder = new LockHolder(to, Duration.ofSeconds(5))) {
                bank.tryTransfer(from, to, 30, Duration.ofMillis(50));
            }

            assertThat(from.lock().isLocked()).as("%s is still locked", from).isFalse();
            assertThat(to.lock().isLocked()).as("%s is still locked", to).isFalse();
        }

        @Test
        @DisplayName("tryTransfer() waits for an account that becomes free before the timeout ends")
        void tryTransferWaitsForBusyAccount() throws InterruptedException {
            var from = new Account(1, 100);
            var to = new Account(2, 100);

            boolean moved;
            try (var holder = new LockHolder(to, Duration.ofMillis(100))) { // lets go after 100 ms
                moved = bank.tryTransfer(from, to, 30, Duration.ofSeconds(2));
            }

            assertThat(moved).isTrue();
            assertThat(from.balance()).isEqualTo(70);
            assertThat(to.balance()).isEqualTo(130);
        }

        @Test
        @DisplayName("tryTransfer() with a zero timeout works when the accounts are free")
        void tryTransferZeroTimeout() throws InterruptedException {
            var from = new Account(1, 100);
            var to = new Account(2, 0);

            assertThat(bank.tryTransfer(from, to, 30, Duration.ZERO)).isTrue();
            assertThat(to.balance()).isEqualTo(30);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("transfer() throws InsufficientFundsException and changes nothing when the balance is too low")
        void transferInsufficientFunds() {
            var from = new Account(1, 50);
            var to = new Account(2, 0);

            assertThatThrownBy(() -> bank.transfer(from, to, 51)).isInstanceOf(InsufficientFundsException.class);

            assertThat(from.balance()).isEqualTo(50);
            assertThat(to.balance()).isZero();
            assertThat(from.lock().isLocked() || to.lock().isLocked()).as("an account is still locked").isFalse();
        }

        @Test
        @DisplayName("tryTransfer() throws InsufficientFundsException, changes nothing and releases both locks")
        void tryTransferInsufficientFunds() {
            var from = new Account(1, 50);
            var to = new Account(2, 0);

            assertThatThrownBy(() -> bank.tryTransfer(from, to, 51, Duration.ofSeconds(1)))
                    .isInstanceOf(InsufficientFundsException.class);

            assertThat(from.balance()).isEqualTo(50);
            assertThat(to.balance()).isZero();
            assertThat(from.lock().isLocked() || to.lock().isLocked()).as("an account is still locked").isFalse();
        }

        @ParameterizedTest(name = "amount {0}")
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        @DisplayName("rejects an amount that isn't positive")
        void rejectsAmountBelowOne(long amount) {
            var from = new Account(1, 100);
            var to = new Account(2, 100);

            assertThatThrownBy(() -> bank.transfer(from, to, amount)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> bank.tryTransfer(from, to, amount, Duration.ofSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(from.balance()).isEqualTo(100);
            assertThat(to.balance()).isEqualTo(100);
        }

        @Test
        @DisplayName("rejects a transfer from an account to itself")
        void rejectsSameAccount() {
            var account = new Account(1, 100);

            assertThatThrownBy(() -> bank.transfer(account, account, 10)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> bank.tryTransfer(account, account, 10, Duration.ofSeconds(1)))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThat(account.balance()).isEqualTo(100);
        }

        @Test
        @DisplayName("rejects null accounts and a null timeout")
        void rejectsNulls() {
            var account = new Account(1, 100);

            assertThatThrownBy(() -> bank.transfer(null, account, 10)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> bank.transfer(account, null, 10)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> bank.tryTransfer(account, new Account(2, 0), 10, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a negative timeout")
        void rejectsNegativeTimeout() {
            var from = new Account(1, 100);
            var to = new Account(2, 0);

            assertThatThrownBy(() -> bank.tryTransfer(from, to, 10, Duration.ofMillis(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("opposite transfers between two accounts on 8 threads finish without a deadlock")
        void oppositeTransfersDoNotDeadlock() {
            var a = new Account(1, 1_000_000);
            var b = new Account(2, 1_000_000);

            // Half of the threads move money from a to b, the other half from b to a, all at the same time.
            Concurrently.run(THREADS, DEADLOCK_TIMEOUT, thread -> {
                for (int i = 0; i < 10_000; i++) {
                    if (thread % 2 == 0) {
                        bank.transfer(a, b, 1);
                    } else {
                        bank.transfer(b, a, 1);
                    }
                }
            });

            assertThat(a.balance()).isEqualTo(1_000_000);
            assertThat(b.balance()).isEqualTo(1_000_000);
        }

        @Test
        @DisplayName("random transfers between 5 accounts on 8 threads keep the total and never overdraw")
        void randomTransfersKeepTheTotal() {
            List<Account> accounts = LongStream.rangeClosed(1, 5).mapToObj(id -> new Account(id, 1_000)).toList();

            Concurrently.run(THREADS, DEADLOCK_TIMEOUT, thread -> {
                var random = new Random(thread); // fixed seed per thread: the same transfers in every run
                for (int i = 0; i < 10_000; i++) {
                    Account from = accounts.get(random.nextInt(5));
                    Account to = accounts.get(random.nextInt(5));
                    if (from == to) {
                        continue;
                    }
                    try {
                        bank.transfer(from, to, 1 + random.nextInt(100));
                    } catch (InsufficientFundsException expected) {
                        // The balances are small on purpose, so this happens now and then.
                    }
                }
            });

            assertThat(accounts).allSatisfy(account -> assertThat(account.balance()).isNotNegative());
            assertThat(accounts.stream().mapToLong(Account::balance).sum()).as("total").isEqualTo(5_000);
        }

        @Test
        @DisplayName("8 threads emptying the same accounts at the same time never overdraw them")
        void emptyingAccountsNeverOverdraws() {
            int accountCount = 500;
            int centsPerAccount = 5;
            List<Account> sources = LongStream.rangeClosed(1, accountCount)
                    .mapToObj(id -> new Account(id, centsPerAccount))
                    .toList();
            var target = new Account(0, 0);
            var transfers = new AtomicInteger();

            // Every thread takes one cent after another from an account until it is empty, then moves on to the
            // next account. So all threads compete for the last cents of every account.
            Concurrently.run(THREADS, DEADLOCK_TIMEOUT, thread -> {
                for (Account source : sources) {
                    boolean empty = false;
                    while (!empty) {
                        try {
                            bank.transfer(source, target, 1);
                            transfers.incrementAndGet();
                        } catch (InsufficientFundsException e) {
                            empty = true;
                        }
                    }
                }
            });

            long overdrawn = sources.stream().filter(source -> source.balance() < 0).count();
            assertThat(overdrawn).as("accounts with a negative balance").isZero();
            assertThat(transfers).as("transfers that succeeded").hasValue(accountCount * centsPerAccount);
            assertThat(target.balance()).isEqualTo(accountCount * centsPerAccount);
        }

        @Test
        @DisplayName("transfer() and tryTransfer() running at the same time never create or lose money")
        void transferAndTryTransferTogether() {
            var a = new Account(1, 1_000_000);
            var b = new Account(2, 1_000_000);
            var c = new Account(3, 1_000_000);
            var transferred = new AtomicLong();
            var tryTransferred = new AtomicLong();

            // Half of the threads use transfer() from a to b, the other half tryTransfer() from b to c.
            // So both methods change the balance of b all the time.
            Concurrently.run(THREADS, DEADLOCK_TIMEOUT, thread -> {
                for (int i = 0; i < 20_000; i++) {
                    if (thread % 2 == 0) {
                        bank.transfer(a, b, 1);
                        transferred.incrementAndGet();
                    } else if (tryTransfer(b, c, 1)) {
                        tryTransferred.incrementAndGet();
                    }
                }
            });

            assertThat(a.balance() + b.balance() + c.balance()).as("total").isEqualTo(3_000_000);
            assertThat(b.balance()).isEqualTo(1_000_000 + transferred.get() - tryTransferred.get());
        }

        @Test
        @DisplayName("opposite tryTransfers between two accounts on 8 threads all succeed and keep the total")
        void oppositeTryTransfersSucceed() {
            var a = new Account(1, 1_000_000);
            var b = new Account(2, 1_000_000);
            var gaveUp = new AtomicInteger();

            Concurrently.run(THREADS, DEADLOCK_TIMEOUT, thread -> {
                for (int i = 0; i < 10_000; i++) {
                    boolean moved = thread % 2 == 0 ? tryTransfer(a, b, 1) : tryTransfer(b, a, 1);
                    if (!moved) {
                        gaveUp.incrementAndGet();
                    }
                }
            });

            assertThat(gaveUp).as("tryTransfers that gave up").hasValue(0);
            assertThat(a.balance()).isEqualTo(1_000_000);
            assertThat(b.balance()).isEqualTo(1_000_000);
        }
    }
}
