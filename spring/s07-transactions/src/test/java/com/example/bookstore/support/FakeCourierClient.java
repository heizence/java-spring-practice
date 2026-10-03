package com.example.bookstore.support;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.example.bookstore.shipping.CourierClient;
import com.example.bookstore.shipping.PickupRequest;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * The courier's API for tests. It answers at once with the tracking number {@code TRK-<order id>} and records every
 * call. A test can make it fail, or hold every call until the test releases it: a slow courier, without sleeping.
 *
 * <p>{@link IntegrationTestConfiguration} registers it as the primary {@link CourierClient}, and
 * {@link IntegrationTestListener} resets it before each test. It is safe to call from several threads at once.
 */
public final class FakeCourierClient implements CourierClient {

    /** A held call fails if the test doesn't release it within this time, so a forgotten release can't hang. */
    private static final Duration HOLD_LIMIT = Duration.ofSeconds(5);

    /**
     * One call to the courier.
     *
     * @param insideTransaction whether the calling thread was inside a database transaction during the call
     */
    public record Call(PickupRequest request, boolean insideTransaction) {
    }

    private final List<Call> calls = new CopyOnWriteArrayList<>();
    private volatile CountDownLatch gate = new CountDownLatch(0);
    private volatile Runnable callListener = () -> {
    };
    private volatile RuntimeException failure;

    @Override
    public String requestPickup(PickupRequest request) {
        calls.add(new Call(request, TransactionSynchronizationManager.isActualTransactionActive()));
        callListener.run();
        waitForRelease();
        RuntimeException plannedFailure = failure;
        if (plannedFailure != null) {
            throw plannedFailure;
        }
        return "TRK-" + request.orderId();
    }

    /** From now on, every call waits until {@link #releaseCalls()}. */
    public void holdCalls() {
        gate = new CountDownLatch(1);
    }

    /** Lets the waiting calls, and all later ones, continue. */
    public void releaseCalls() {
        gate.countDown();
    }

    /** From now on, every call throws this exception (after waiting, if calls are held). */
    public void failWith(RuntimeException exception) {
        failure = Objects.requireNonNull(exception, "exception");
    }

    /** Runs {@code listener} on the calling thread at the start of every call, before a held call waits. */
    public void onCall(Runnable listener) {
        callListener = Objects.requireNonNull(listener, "listener");
    }

    /** The calls so far, oldest first. */
    public List<Call> calls() {
        return List.copyOf(calls);
    }

    /** Forgets the calls and goes back to answering at once, without failing. */
    public void reset() {
        releaseCalls();
        calls.clear();
        gate = new CountDownLatch(0);
        callListener = () -> {
        };
        failure = null;
    }

    private void waitForRelease() {
        try {
            if (!gate.await(HOLD_LIMIT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("The test didn't release this courier call within " + HOLD_LIMIT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while the courier call was held", e);
        }
    }
}
