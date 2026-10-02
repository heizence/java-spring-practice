package practice.j08.ex01;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Calls a slow lookup, such as a remote service, for many ids at once on a fixed-size thread pool.
 *
 * <h2>Rules for {@link #fetchAll}</h2>
 * <ul>
 *   <li>{@code fetcher} is called once per id, on a pool of {@code threads} threads, never on the calling thread.
 *       Up to {@code threads} calls run at the same time, and never more.</li>
 *   <li>The results come back in the order of {@code ids}, not in the order in which the calls finish. The list
 *       can't be modified.</li>
 *   <li>If a call throws, {@code fetchAll} throws a {@link FetchException} for the first failing id in the order
 *       of {@code ids}. Its cause is the exception that the fetcher threw, not a wrapper around it.</li>
 *   <li>Once {@code fetchAll} knows that it will throw, it cancels the calls that are still waiting or running:
 *       waiting calls never start, and running calls are interrupted.</li>
 *   <li>The pool is shut down before {@code fetchAll} returns or throws, so no pool thread is left running.</li>
 *   <li>An empty list returns an empty list without calling {@code fetcher}.</li>
 *   <li>{@code threads} below 1 throws {@link IllegalArgumentException}. A {@code null} list, id or fetcher throws
 *       {@link NullPointerException}. Both happen before any call starts.</li>
 * </ul>
 */
public final class ParallelFetcher {

    private ParallelFetcher() {
    }

    /**
     * Calls {@code fetcher} for every id on a pool of {@code threads} threads and returns the results in the order
     * of {@code ids}.
     *
     * @throws FetchException       if a call fails (see the class description)
     * @throws InterruptedException if the calling thread is interrupted while it waits for the results
     */
    public static List<Integer> fetchAll(List<String> ids, Function<String, Integer> fetcher, int threads)
            throws InterruptedException {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be at least 1, but was " + threads);
        }
        // List.copyOf throws NullPointerException for a null list or a null id.
        List<String> checkedIds = List.copyOf(ids);
        Objects.requireNonNull(fetcher, "fetcher");

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        List<Future<Integer>> futures = new ArrayList<>(checkedIds.size());
        for (String id : checkedIds) {
            futures.add(pool.submit(() -> fetcher.apply(id)));
        }

        List<Integer> results = new ArrayList<>(checkedIds.size());
        for (int i = 0; i < futures.size(); i++) {
            try {
                results.add(futures.get(i).get());
            } catch (ExecutionException e) {
                throw new FetchException(checkedIds.get(i), e.getCause());
            }
        }
        pool.shutdown();
        return Collections.unmodifiableList(results);
    }
}
