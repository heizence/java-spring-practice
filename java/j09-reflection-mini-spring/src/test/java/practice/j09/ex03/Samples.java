package practice.j09.ex03;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.LongSupplier;

/** Interfaces and fakes for {@link ProxiesTest}. */
final class Samples {

    private Samples() {
    }

    /** A checked exception: an expected business outcome, not a bug. */
    public static class AccountLockedException extends Exception {

        public AccountLockedException(String account) {
            super("account " + account + " is locked");
        }
    }

    /** The interface that the transactional proxy implements. */
    public interface AccountService {

        /** Annotated here, on the interface. Throws IllegalArgumentException for an amount that isn't positive. */
        @Transactional
        void transfer(String from, String to, long cents);

        /** Annotated on the implementation only. Throws the checked AccountLockedException for a locked account. */
        void close(String account) throws AccountLockedException;

        /** Not transactional. Throws NoSuchElementException for the account "unknown". */
        long balance(String account);

        /** Not transactional, but it calls transfer() on itself. */
        void payday(String account, long cents);

        /** Throws an Error. */
        @Transactional
        void corrupt();
    }

    /** Writes each call into a shared list of events, next to the transaction events. */
    public static class RecordingAccountService implements AccountService {

        private final List<String> events;

        public RecordingAccountService(List<String> events) {
            this.events = events;
        }

        @Override
        public void transfer(String from, String to, long cents) {
            if (cents <= 0) {
                throw new IllegalArgumentException("amount must be positive: " + cents);
            }
            events.add("transfer " + from + "->" + to + " " + cents);
        }

        @Transactional
        @Override
        public void close(String account) throws AccountLockedException {
            events.add("close " + account);
            if (account.startsWith("locked")) {
                throw new AccountLockedException(account);
            }
        }

        @Override
        public long balance(String account) {
            if (account.equals("unknown")) {
                throw new NoSuchElementException("no account " + account);
            }
            events.add("balance " + account);
            return 1_000;
        }

        @Override
        public void payday(String account, long cents) {
            events.add("payday " + account);
            transfer("company", account, cents); // this.transfer(...): a call on the target, not on the proxy
        }

        @Override
        public void corrupt() {
            events.add("corrupt");
            throw new AssertionError("ledger is out of balance");
        }

        @Override
        public String toString() {
            return "RecordingAccountService";
        }
    }

    /** Writes begin, commit and rollback into the same list as the service. */
    public static class RecordingTxManager implements TxManager {

        private final List<String> events;

        public RecordingTxManager(List<String> events) {
            this.events = events;
        }

        @Override
        public void begin() {
            events.add("begin");
        }

        @Override
        public void commit() {
            events.add("commit");
        }

        @Override
        public void rollback() {
            events.add("rollback");
        }
    }

    /** A clock in nanoseconds that only moves when someone moves it. */
    public static class FakeTicker implements LongSupplier {

        private long nanos = 1_000_000;

        @Override
        public long getAsLong() {
            return nanos;
        }

        public void advance(long nanos) {
            this.nanos += nanos;
        }
    }

    /** The interface that the timing proxy implements. */
    public interface PriceService {

        long price(String sku);

        void refresh() throws IOException;

        /** A default method that calls price() on itself. */
        default String describe(String sku) {
            return sku + " costs " + price(sku);
        }
    }

    /** Each method moves the fake clock by a known amount, so the tests know how long a call "takes". */
    public static class SlowPriceService implements PriceService {

        private final FakeTicker ticker;

        public SlowPriceService(FakeTicker ticker) {
            this.ticker = ticker;
        }

        @Override
        public long price(String sku) {
            ticker.advance(250);
            if (sku.isBlank()) {
                throw new IllegalArgumentException("blank sku");
            }
            return 999;
        }

        @Override
        public void refresh() throws IOException {
            ticker.advance(100);
            throw new IOException("price feed offline");
        }

        @Override
        public String toString() {
            return "SlowPriceService";
        }
    }
}
