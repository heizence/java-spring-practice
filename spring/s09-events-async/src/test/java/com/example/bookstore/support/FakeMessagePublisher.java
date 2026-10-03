package com.example.bookstore.support;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.example.bookstore.outbox.Message;
import com.example.bookstore.outbox.MessagePublisher;

/**
 * The message broker for tests. It records every published message, can refuse the messages of chosen events (a
 * broker that fails for them), and can hold one call until the test releases it (a slow broker, without sleeping).
 *
 * <p>{@link IntegrationTestConfiguration} registers it as the primary {@link MessagePublisher}, and
 * {@link IntegrationTestListener} resets it before each test. It is safe to call from several threads at once.
 */
public final class FakeMessagePublisher implements MessagePublisher {

    /** A held call fails if the test doesn't release it within this time, so a forgotten release can't hang. */
    private static final Duration HOLD_LIMIT = Duration.ofSeconds(5);

    private final List<Message> published = new CopyOnWriteArrayList<>();
    private final Set<Long> refusedEventIds = ConcurrentHashMap.newKeySet();
    /** The hold that the next call takes; {@code null} once a call has taken it. */
    private final AtomicReference<Hold> untakenHold = new AtomicReference<>();
    /** The latest hold, taken or not, so that the test can wait for it and release it. */
    private volatile Hold latestHold;

    private record Hold(CountDownLatch arrived, CountDownLatch released) {
    }

    @Override
    public void publish(Message message) {
        Hold hold = untakenHold.getAndSet(null); // only one call takes the hold
        if (hold != null) {
            hold.arrived().countDown();
            waitForRelease(hold.released());
        }
        if (refusedEventIds.contains(message.eventId())) {
            throw new IllegalStateException("The broker refused event " + message.eventId());
        }
        published.add(message);
    }

    /** The messages published so far, in the order the broker received them. */
    public List<Message> published() {
        return List.copyOf(published);
    }

    /** From now on, publishing the event with this id fails, until {@link #recover()}. */
    public void refuse(long eventId) {
        refusedEventIds.add(eventId);
    }

    /** Publishing works for every event again. */
    public void recover() {
        refusedEventIds.clear();
    }

    /** The next call waits until {@link #releaseHeldCall()}; the calls after it don't wait. */
    public void holdNextCall() {
        var hold = new Hold(new CountDownLatch(1), new CountDownLatch(1));
        latestHold = hold;
        untakenHold.set(hold);
    }

    /**
     * Waits until the call that {@link #holdNextCall()} prepared for has arrived and is waiting.
     *
     * @return {@code false} if no call arrived within the timeout
     */
    public boolean awaitHeldCall(Duration timeout) throws InterruptedException {
        Hold hold = latestHold;
        return hold != null && hold.arrived().await(timeout.toMillis(), TimeUnit.MILLISECONDS);
    }

    /** Lets the held call continue. Harmless if no call is held. */
    public void releaseHeldCall() {
        Hold hold = latestHold;
        if (hold != null) {
            hold.released().countDown();
        }
    }

    /** Forgets the messages, publishes every event again, and lets a held call continue. */
    public void reset() {
        releaseHeldCall();
        untakenHold.set(null);
        latestHold = null;
        published.clear();
        refusedEventIds.clear();
    }

    private static void waitForRelease(CountDownLatch released) {
        try {
            if (!released.await(HOLD_LIMIT.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("The test didn't release this publish call within " + HOLD_LIMIT);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while the publish call was held", e);
        }
    }
}
