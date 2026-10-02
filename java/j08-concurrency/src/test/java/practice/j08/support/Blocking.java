package practice.j08.support;

import java.time.Duration;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Helpers for J08 tests that make threads wait. They keep test code short by turning the checked
 * {@link InterruptedException} into an unchecked one (after restoring the thread's interrupt flag), and they never
 * wait forever.
 */
public final class Blocking {

    /** How long a test waits for something that should happen almost at once. */
    public static final Duration PATIENCE = Duration.ofSeconds(2);

    private Blocking() {
    }

    /** Sleeps for {@code duration}. */
    public static void sleep(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw interrupted(e);
        }
    }

    /** Waits until {@code latch} opens, and fails if that takes longer than {@link #PATIENCE}. */
    public static void await(CountDownLatch latch) {
        try {
            if (!latch.await(PATIENCE.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new AssertionError("Waited " + PATIENCE.toSeconds() + " s for something that never happened");
            }
        } catch (InterruptedException e) {
            throw interrupted(e);
        }
    }

    /**
     * Waits at {@code barrier} for the other threads, for at most {@link #PATIENCE}.
     *
     * @return {@code true} if all parties met at the barrier, so they were running at the same time
     */
    public static boolean allArrive(CyclicBarrier barrier) {
        try {
            barrier.await(PATIENCE.toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (TimeoutException | BrokenBarrierException e) {
            return false;
        } catch (InterruptedException e) {
            throw interrupted(e);
        }
    }

    /** Runs {@code code} on a new daemon thread and returns its outcome as a future. */
    public static <T> CompletableFuture<T> inBackground(Callable<T> code) {
        var outcome = new CompletableFuture<T>();
        // A daemon thread can't keep the test JVM alive, even if the code under test never returns.
        Thread.ofPlatform().daemon().name("test-background").start(() -> {
            try {
                outcome.complete(code.call());
            } catch (Throwable e) {
                outcome.completeExceptionally(e);
            }
        });
        return outcome;
    }

    private static IllegalStateException interrupted(InterruptedException e) {
        Thread.currentThread().interrupt();
        return new IllegalStateException("Interrupted while waiting", e);
    }
}
