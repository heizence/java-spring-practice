package practice.testkit;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.IntConsumer;

/**
 * Runs code on many threads at once, for tests that hunt race conditions.
 *
 * <p>All threads wait at a starting gate and are released together, which makes races far more
 * likely to show up than starting threads one by one. The threads are daemon threads, so a deadlocked
 * task cannot keep the test JVM alive.
 */
public final class Concurrently {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(8);

    private Concurrently() {
    }

    /**
     * Runs {@code task} on {@code threads} threads that start at the same moment and waits for all of them.
     * Each call receives its thread index, from 0 to {@code threads - 1}.
     *
     * @throws AssertionError if the threads don't finish within 8 seconds (for example, a deadlock), or
     *                        wrapping the first exception thrown by any task
     */
    public static void run(int threads, IntConsumer task) {
        run(threads, DEFAULT_TIMEOUT, task);
    }

    /** Same as {@link #run(int, IntConsumer)} with a custom timeout. */
    public static void run(int threads, Duration timeout, IntConsumer task) {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1");
        }
        var ready = new CountDownLatch(threads);
        var start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(threads, runnable -> {
            var thread = new Thread(runnable);
            thread.setDaemon(true);
            return thread;
        });
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                int index = i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    task.accept(index);
                    return null;
                }));
            }
            await(ready, timeout);
            start.countDown();
            long deadline = System.nanoTime() + timeout.toNanos();
            for (Future<?> future : futures) {
                future.get(Math.max(0, deadline - System.nanoTime()), TimeUnit.NANOSECONDS);
            }
        } catch (TimeoutException e) {
            throw new AssertionError("The threads did not finish within " + timeout.toSeconds()
                    + " seconds. Is there a deadlock or an endless loop?", e);
        } catch (ExecutionException e) {
            throw new AssertionError("A thread failed: " + e.getCause(), e.getCause());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("Interrupted while waiting for the threads", e);
        } finally {
            pool.shutdownNow();
        }
    }

    private static void await(CountDownLatch latch, Duration timeout) throws InterruptedException, TimeoutException {
        if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            throw new TimeoutException();
        }
    }
}
