package practice.j09.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.j09.ex03.Samples.AccountLockedException;
import practice.j09.ex03.Samples.AccountService;
import practice.j09.ex03.Samples.FakeTicker;
import practice.j09.ex03.Samples.PriceService;
import practice.j09.ex03.Samples.RecordingAccountService;
import practice.j09.ex03.Samples.RecordingTxManager;
import practice.j09.ex03.Samples.SlowPriceService;

@DisplayName("J09-03 · Dynamic proxies")
class ProxiesTest {

    private final FakeTicker ticker = new FakeTicker();
    private final List<Timing> timings = new ArrayList<>();
    private final SlowPriceService priceTarget = new SlowPriceService(ticker);

    /** The service and the transaction manager write into this one list, so tests can check the order. */
    private final List<String> events = new ArrayList<>();
    private final RecordingAccountService accountTarget = new RecordingAccountService(events);
    private final RecordingTxManager txManager = new RecordingTxManager(events);

    private PriceService timedPrices() {
        return Proxies.timing(PriceService.class, priceTarget, ticker, timings::add);
    }

    private AccountService transactionalAccounts() {
        return Proxies.transactional(AccountService.class, accountTarget, txManager);
    }

    private static Timing timing(String method, long nanos) {
        return new Timing(method, Duration.ofNanos(nanos));
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("timing() returns the target's result")
        void timingReturnsResult() {
            assertThat(timedPrices().price("A-1")).isEqualTo(999);
        }

        @Test
        @DisplayName("timing() reports the method's name and how long the call took")
        void timingReportsCall() {
            timedPrices().price("A-1");

            assertThat(timings).containsExactly(timing("price", 250));
        }

        @Test
        @DisplayName("timing() measures every call, in call order")
        void timingEveryCall() {
            PriceService prices = timedPrices();

            prices.price("A-1");
            prices.price("B-2");

            assertThat(timings).containsExactly(timing("price", 250), timing("price", 250));
        }

        @Test
        @DisplayName("transactional() runs a @Transactional method between begin and commit")
        void transactionalCommits() {
            transactionalAccounts().transfer("a", "b", 100);

            assertThat(events).containsExactly("begin", "transfer a->b 100", "commit");
        }

        @Test
        @DisplayName("transactional() calls a method without @Transactional directly and returns its result")
        void nonTransactionalPassesThrough() {
            long balance = transactionalAccounts().balance("a");

            assertThat(balance).isEqualTo(1_000);
            assertThat(events).containsExactly("balance a");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("timing() also measures a call that throws, and rethrows the original exception")
        void timingFailedCall() {
            PriceService prices = timedPrices();

            assertThatThrownBy(() -> prices.price(" "))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("blank sku");
            assertThat(timings).containsExactly(timing("price", 250));
        }

        @Test
        @DisplayName("timing() rethrows a checked exception unchanged")
        void timingCheckedException() {
            PriceService prices = timedPrices();

            assertThatThrownBy(prices::refresh)
                    .isInstanceOf(IOException.class)
                    .hasMessage("price feed offline");
            assertThat(timings).containsExactly(timing("refresh", 100));
        }

        @Test
        @DisplayName("timing() times a default method, but not the calls it makes on the target itself")
        void timingDefaultMethod() {
            String description = timedPrices().describe("A-1");

            assertThat(description).isEqualTo("A-1 costs 999");
            assertThat(timings).containsExactly(timing("describe", 250));
        }

        @Test
        @DisplayName("toString() returns the target's toString() and isn't timed")
        void timingToString() {
            assertThat(timedPrices().toString()).isEqualTo("SlowPriceService");
            assertThat(timings).isEmpty();
        }

        @Test
        @DisplayName("a proxy equals itself only, and its hashCode() is its identity hash code")
        void timingEqualsAndHashCode() {
            PriceService prices = timedPrices();

            assertThat(prices.equals(prices)).isTrue();
            assertThat(prices.equals(priceTarget)).isFalse();
            assertThat(prices.equals(timedPrices())).isFalse();
            assertThat(prices.hashCode()).isEqualTo(System.identityHashCode(prices));
            assertThat(timings).isEmpty();
        }

        @Test
        @DisplayName("transactional() also finds @Transactional on the implementation's method")
        void annotationOnImplementation() throws AccountLockedException {
            transactionalAccounts().close("acc-1");

            assertThat(events).containsExactly("begin", "close acc-1", "commit");
        }

        @Test
        @DisplayName("transactional() rolls back and rethrows when the method throws a RuntimeException")
        void rollbackOnRuntimeException() {
            AccountService accounts = transactionalAccounts();

            assertThatThrownBy(() -> accounts.transfer("a", "b", -5))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("amount must be positive: -5");
            assertThat(events).containsExactly("begin", "rollback");
        }

        @Test
        @DisplayName("transactional() rolls back when the method throws an Error")
        void rollbackOnError() {
            AccountService accounts = transactionalAccounts();

            assertThatThrownBy(accounts::corrupt).isInstanceOf(AssertionError.class);
            assertThat(events).containsExactly("begin", "corrupt", "rollback");
        }

        @Test
        @DisplayName("transactional() commits when the method throws a checked exception, then rethrows it")
        void commitOnCheckedException() {
            AccountService accounts = transactionalAccounts();

            assertThatThrownBy(() -> accounts.close("locked-7"))
                    .isInstanceOf(AccountLockedException.class)
                    .hasMessage("account locked-7 is locked");
            assertThat(events).containsExactly("begin", "close locked-7", "commit");
        }

        @Test
        @DisplayName("transactional() rethrows the exceptions of methods without @Transactional unchanged")
        void nonTransactionalException() {
            AccountService accounts = transactionalAccounts();

            assertThatThrownBy(() -> accounts.balance("unknown")).isInstanceOf(NoSuchElementException.class);
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("self-invocation: the target's call to its own @Transactional method gets no transaction")
        void selfInvocationBypassesProxy() {
            transactionalAccounts().payday("a", 100);

            assertThat(events).containsExactly("payday a", "transfer company->a 100");
        }

        @Test
        @DisplayName("toString(), equals() and hashCode() never start a transaction")
        void objectMethodsWithoutTransaction() {
            AccountService accounts = transactionalAccounts();

            assertThat(accounts.toString()).isEqualTo("RecordingAccountService");
            assertThat(accounts.equals(accounts)).isTrue();
            assertThat(accounts.hashCode()).isEqualTo(System.identityHashCode(accounts));
            assertThat(events).isEmpty();
        }

        @Test
        @DisplayName("the proxies can be stacked: timing around transactional")
        void proxiesStack() {
            AccountService accounts =
                    Proxies.timing(AccountService.class, transactionalAccounts(), ticker, timings::add);

            accounts.transfer("a", "b", 100);

            assertThat(events).containsExactly("begin", "transfer a->b 100", "commit");
            assertThat(timings).extracting(Timing::method).containsExactly("transfer");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("both proxies reject a class: JDK proxies only implement interfaces")
        void rejectClasses() {
            assertThatThrownBy(() -> Proxies.timing(SlowPriceService.class, priceTarget, ticker, timings::add))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> Proxies.transactional(RecordingAccountService.class, accountTarget, txManager))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("timing() rejects null arguments")
        void timingRejectsNull() {
            assertThatThrownBy(() -> Proxies.timing(null, priceTarget, ticker, timings::add))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Proxies.timing(PriceService.class, null, ticker, timings::add))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Proxies.timing(PriceService.class, priceTarget, null, timings::add))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Proxies.timing(PriceService.class, priceTarget, ticker, null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("transactional() rejects null arguments")
        void transactionalRejectsNull() {
            assertThatThrownBy(() -> Proxies.transactional(null, accountTarget, txManager))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Proxies.transactional(AccountService.class, null, txManager))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Proxies.transactional(AccountService.class, accountTarget, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
