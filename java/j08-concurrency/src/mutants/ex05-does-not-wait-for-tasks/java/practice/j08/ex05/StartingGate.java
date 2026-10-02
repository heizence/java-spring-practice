package practice.j08.ex05;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntConsumer;

/**
 * Starts several tasks at the same moment and waits until all of them are done, the way load tests and tests for
 * race conditions do. ({@code practice.testkit.Concurrently}, which the J08 tests use, works the same way.)
 *
 * <h2>Rules for {@link #runTogether}</h2>
 * <ul>
 *   <li>Starts {@code threads} new platform threads. Thread {@code i} calls {@code task.accept(i)} once.</li>
 *   <li>No task begins before every thread has been started: the threads wait at a starting gate, a
 *       {@link java.util.concurrent.CountDownLatch CountDownLatch} with count 1 that opens only after the last thread
 *       has been started.</li>
 *   <li>{@code runTogether} returns when every task has finished. It waits on a second {@code CountDownLatch}, with
 *       count {@code threads}, that each thread counts down in a {@code finally} block. So a task that throws still
 *       counts as finished.</li>
 *   <li>It returns the number of tasks that threw a {@link RuntimeException}.</li>
 *   <li>{@code threads} below 1 throws {@link IllegalArgumentException}. A {@code null} task throws
 *       {@link NullPointerException}.</li>
 * </ul>
 */
public final class StartingGate {

    private StartingGate() {
    }

    /**
     * Runs {@code task} on {@code threads} threads that start at the same moment, and waits for all of them.
     *
     * @return the number of tasks that threw a {@link RuntimeException}
     * @throws InterruptedException if the calling thread is interrupted while it waits
     */
    public static int runTogether(int threads, IntConsumer task) throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1, but was " + threads);
        }
        Objects.requireNonNull(task, "task");

        var gate = new CountDownLatch(1);           // opens once, for everybody: the starting signal
        var finished = new CountDownLatch(threads); // reaches 0 when every task is done
        var failures = new AtomicInteger();

        for (int i = 0; i < threads; i++) {
            int index = i; // a lambda can only use local variables that never change
            Thread.ofPlatform().name("gate-", i).start(() -> {
                try {
                    gate.await();
                    task.accept(index);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // nobody interrupts these threads; if someone does, stop
                } catch (RuntimeException e) {
                    failures.incrementAndGet();
                } finally {
                    // In a finally block, so that every way out of the task counts down, even an Error.
                    // Otherwise runTogether would wait for this thread forever.
                    finished.countDown();
                }
            });
        }

        gate.countDown(); // every thread has been started: let them all go
        return failures.get();
    }
}
