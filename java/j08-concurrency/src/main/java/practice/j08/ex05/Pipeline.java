package practice.j08.ex05;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * Passes items from one producer to several consumers through a bounded queue.
 *
 * <p>The producer is the thread that calls {@link #process}: it reads items from {@code source} and puts them into an
 * {@link java.util.concurrent.ArrayBlockingQueue ArrayBlockingQueue}. {@code consumers} threads take items from the
 * queue and pass each one to {@code worker}. Because the queue is bounded, a slow worker makes the producer wait,
 * instead of letting unprocessed items pile up in memory. This is called <em>backpressure</em>.
 *
 * <h2>Rules for {@link #process}</h2>
 * <ul>
 *   <li>Every item from {@code source} is passed to {@code worker} exactly once, on one of the {@code consumers}
 *       consumer threads. The consumers work at the same time.</li>
 *   <li>The queue holds at most {@code capacity} items. When it is full, the producer waits ({@code put}). It reads
 *       the next item from {@code source} only after it has put the current one into the queue.</li>
 *   <li>The queue is first in, first out: with one consumer, the items are processed in the order of
 *       {@code source}.</li>
 *   <li>When {@code source} has no more items, the producer puts one end marker (a "poison pill") per consumer into
 *       the queue. A consumer stops when it takes an end marker.</li>
 *   <li>{@code process} returns after every item has been processed and every consumer thread has finished.</li>
 *   <li>{@code worker} must not throw. (Handling worker failures is left out of this exercise.)</li>
 *   <li>{@code capacity} or {@code consumers} below 1 throws {@link IllegalArgumentException}. A {@code null} source
 *       or worker throws {@link NullPointerException}.</li>
 * </ul>
 */
public final class Pipeline {

    private Pipeline() {
    }

    /**
     * Passes every item of {@code source} to {@code worker} on {@code consumers} threads, through a queue that holds
     * at most {@code capacity} items.
     *
     * @throws InterruptedException if the calling thread is interrupted while it waits
     */
    public static <T> void process(Iterator<T> source, int capacity, int consumers, Consumer<T> worker)
            throws InterruptedException {
        // TODO: J08-05
        throw new UnsupportedOperationException("TODO: J08-05");
    }
}
