package practice.j08.ex05;

import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(worker, "worker");
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1, but was " + capacity);
        }
        if (consumers < 1) {
            throw new IllegalArgumentException("consumers must be at least 1, but was " + consumers);
        }

        BlockingQueue<Message<T>> queue = new ArrayBlockingQueue<>(capacity);
        try (ExecutorService pool = Executors.newFixedThreadPool(consumers)) {
            for (int i = 0; i < consumers; i++) {
                pool.execute(() -> consume(queue, worker));
            }
            try {
                while (source.hasNext()) {
                    queue.offer(new Message.Item<>(source.next()));
                }
                // One end marker per consumer: each consumer stops after it has taken exactly one.
                for (int i = 0; i < consumers; i++) {
                    queue.put(new Message.End<>());
                }
            } catch (InterruptedException e) {
                // We won't produce any more, so the consumers would wait in take() forever, and close() with them.
                // shutdownNow() interrupts them.
                pool.shutdownNow();
                throw e;
            }
        } // close() waits until every consumer has taken its end marker and stopped
    }

    private static <T> void consume(BlockingQueue<Message<T>> queue, Consumer<T> worker) {
        try {
            while (true) {
                // take() waits while the queue is empty.
                switch (queue.take()) {
                    case Message.Item<T>(T item) -> worker.accept(item);
                    case Message.End<T>() -> {
                        return;
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // the pool is being shut down: stop
        }
    }

    /**
     * What goes through the queue: an item, or the end marker that tells a consumer to stop. (A sealed interface with
     * records works like a union type in TypeScript, and the switch in {@link #consume} must handle both cases.)
     */
    private sealed interface Message<T> {

        record Item<T>(T value) implements Message<T> {
        }

        record End<T>() implements Message<T> {
        }
    }
}
