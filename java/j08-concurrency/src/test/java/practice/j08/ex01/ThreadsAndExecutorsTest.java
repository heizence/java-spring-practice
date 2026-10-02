package practice.j08.ex01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import practice.j08.support.Blocking;

@DisplayName("J08-01 · Threads and executors")
class ThreadsAndExecutorsTest {

    /** {@code count} tasks; task {@code i} calls {@code body.accept(i)}. */
    private static List<Runnable> tasks(int count, IntConsumer body) {
        List<Runnable> tasks = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            int index = i;
            tasks.add(() -> body.accept(index));
        }
        return tasks;
    }

    private static <T> List<T> toList(AtomicReferenceArray<T> array) {
        return IntStream.range(0, array.length()).mapToObj(array::get).toList();
    }

    /** Fails unless every thread stops within a short time. */
    private static void assertAllStop(Set<Thread> threads) throws InterruptedException {
        for (Thread thread : threads) {
            assertThat(thread.join(Blocking.PATIENCE)).as("pool thread %s has stopped", thread.getName()).isTrue();
        }
    }

    @Nested
    @DisplayName("ThreadBasics.runAll")
    class RunAll {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("runAll runs every task exactly once")
            void runsEveryTaskOnce() throws InterruptedException {
                Map<Integer, Integer> runs = new ConcurrentHashMap<>();

                ThreadBasics.runAll(tasks(5, i -> runs.merge(i, 1, Integer::sum)));

                assertThat(runs).containsOnlyKeys(0, 1, 2, 3, 4).allSatisfy((task, count) -> assertThat(count).isOne());
            }

            @Test
            @DisplayName("runAll runs each task on its own new thread, never on the calling thread")
            void runsEachTaskOnItsOwnThread() throws InterruptedException {
                var threads = new AtomicReferenceArray<Thread>(3);

                ThreadBasics.runAll(tasks(3, i -> threads.set(i, Thread.currentThread())));

                assertThat(toList(threads)).doesNotContainNull()
                        .doesNotHaveDuplicates()
                        .doesNotContain(Thread.currentThread());
            }

            @Test
            @DisplayName("runAll names the threads worker-0, worker-1, … in task order")
            void namesThreads() throws InterruptedException {
                var names = new AtomicReferenceArray<String>(3);

                ThreadBasics.runAll(tasks(3, i -> names.set(i, Thread.currentThread().getName())));

                assertThat(toList(names)).containsExactly("worker-0", "worker-1", "worker-2");
            }

            @Test
            @DisplayName("runAll returns only after every task has finished")
            void waitsForEveryTask() throws InterruptedException {
                var finished = new AtomicInteger();

                List<Thread> threads = ThreadBasics.runAll(tasks(3, i -> {
                    Blocking.sleep(Duration.ofMillis(100));
                    finished.incrementAndGet();
                }));

                assertThat(finished).hasValue(3);
                assertThat(threads).noneMatch(Thread::isAlive);
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("runAll runs the tasks at the same time, not one after another")
            void runsTasksAtTheSameTime() throws InterruptedException {
                // The barrier opens only when all 4 tasks are waiting at it, so all 4 must be running at once.
                var barrier = new CyclicBarrier(4);
                var metTheOthers = new AtomicInteger();

                ThreadBasics.runAll(tasks(4, i -> {
                    if (Blocking.allArrive(barrier)) {
                        metTheOthers.incrementAndGet();
                    }
                }));

                assertThat(metTheOthers).as("tasks that were running together with all the others").hasValue(4);
            }

            @Test
            @DisplayName("runAll returns the threads in task order")
            void returnsThreadsInTaskOrder() throws InterruptedException {
                var threads = new AtomicReferenceArray<Thread>(3);

                List<Thread> returned = ThreadBasics.runAll(tasks(3, i -> threads.set(i, Thread.currentThread())));

                assertThat(returned).containsExactlyElementsOf(toList(threads));
            }

            @Test
            @DisplayName("runAll with no tasks starts no thread and returns an empty list")
            void noTasks() throws InterruptedException {
                assertThat(ThreadBasics.runAll(List.of())).isEmpty();
            }

            @Test
            @DisplayName("runAll throws InterruptedException when the waiting thread is interrupted")
            void throwsWhenInterrupted() {
                var release = new CountDownLatch(1);
                var outcome = new CompletableFuture<Throwable>();
                Thread caller = Thread.ofPlatform().daemon().start(() -> {
                    try {
                        ThreadBasics.runAll(List.of(() -> Blocking.await(release)));
                        outcome.complete(null);
                    } catch (Throwable e) {
                        outcome.complete(e);
                    }
                });

                caller.interrupt();

                try {
                    assertThat(outcome).succeedsWithin(Blocking.PATIENCE)
                            .as("what runAll threw")
                            .isInstanceOf(InterruptedException.class);
                } finally {
                    release.countDown(); // lets the task finish
                }
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("runAll rejects a null list")
            void rejectsNullList() {
                assertThatThrownBy(() -> ThreadBasics.runAll(null)).isInstanceOf(NullPointerException.class);
            }

            @Test
            @DisplayName("runAll rejects a null task before it starts any thread")
            void rejectsNullTaskBeforeStarting() {
                var ran = new AtomicBoolean();
                List<Runnable> tasks = Arrays.asList(() -> ran.set(true), null);

                assertThatThrownBy(() -> ThreadBasics.runAll(tasks)).isInstanceOf(NullPointerException.class);

                Blocking.sleep(Duration.ofMillis(200)); // a thread started anyway would have run its task by now
                assertThat(ran).as("the first task ran").isFalse();
            }
        }
    }

    @Nested
    @DisplayName("ParallelFetcher.fetchAll")
    class FetchAll {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("fetchAll returns one result per id, in the order of the ids")
            void returnsResultsInOrder() throws InterruptedException {
                List<Integer> results = ParallelFetcher.fetchAll(List.of("30", "10", "20"), Integer::valueOf, 2);

                assertThat(results).containsExactly(30, 10, 20);
            }

            @Test
            @DisplayName("fetchAll calls the fetcher once per id")
            void callsFetcherOncePerId() throws InterruptedException {
                Map<String, Integer> calls = new ConcurrentHashMap<>();

                ParallelFetcher.fetchAll(List.of("a", "b", "c", "d"), id -> calls.merge(id, 1, Integer::sum), 2);

                assertThat(calls).containsOnlyKeys("a", "b", "c", "d")
                        .allSatisfy((id, count) -> assertThat(count).isOne());
            }

            @Test
            @DisplayName("fetchAll calls the fetcher on pool threads, never on the calling thread")
            void callsFetcherOnPoolThreads() throws InterruptedException {
                Set<Thread> threads = ConcurrentHashMap.newKeySet();

                ParallelFetcher.fetchAll(List.of("a", "b", "c"), id -> {
                    threads.add(Thread.currentThread());
                    return 1;
                }, 2);

                assertThat(threads).isNotEmpty().doesNotContain(Thread.currentThread());
            }

            @Test
            @DisplayName("fetchAll runs `threads` calls at the same time")
            void runsCallsAtTheSameTime() throws InterruptedException {
                // The barrier opens only when all 3 calls are waiting at it, so all 3 must be running at once.
                var barrier = new CyclicBarrier(3);
                var metTheOthers = new AtomicInteger();

                ParallelFetcher.fetchAll(List.of("a", "b", "c"), id -> {
                    if (Blocking.allArrive(barrier)) {
                        metTheOthers.incrementAndGet();
                    }
                    return 1;
                }, 3);

                assertThat(metTheOthers).as("calls that were running together with all the others").hasValue(3);
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("fetchAll keeps the order of the ids when later calls finish first")
            void keepsOrderWhenLaterCallsFinishFirst() throws InterruptedException {
                // Call k waits until call k + 1 has finished, then takes a little longer. So the calls finish in
                // reverse order: 3, 2, 1, 0.
                List<CountDownLatch> finished = Stream.generate(() -> new CountDownLatch(1)).limit(4).toList();
                Function<String, Integer> fetcher = id -> {
                    int k = Integer.parseInt(id);
                    if (k < 3) {
                        Blocking.await(finished.get(k + 1));
                        Blocking.sleep(Duration.ofMillis(30));
                    }
                    finished.get(k).countDown();
                    return k * 10;
                };

                List<Integer> results = ParallelFetcher.fetchAll(List.of("0", "1", "2", "3"), fetcher, 4);

                assertThat(results).containsExactly(0, 10, 20, 30);
            }

            @Test
            @DisplayName("fetchAll never runs more than `threads` calls at the same time")
            void neverRunsMoreThanThreadsCalls() throws InterruptedException {
                var running = new AtomicInteger();
                var mostAtOnce = new AtomicInteger();
                List<String> ids = IntStream.range(0, 16).mapToObj(String::valueOf).toList();

                ParallelFetcher.fetchAll(ids, id -> {
                    mostAtOnce.accumulateAndGet(running.incrementAndGet(), Math::max);
                    Blocking.sleep(Duration.ofMillis(30));
                    running.decrementAndGet();
                    return 1;
                }, 3);

                assertThat(mostAtOnce).as("most calls running at the same time").hasValueLessThanOrEqualTo(3);
            }

            @Test
            @DisplayName("fetchAll returns an empty list for no ids, without calling the fetcher")
            void noIds() throws InterruptedException {
                var calls = new AtomicInteger();

                List<Integer> results = ParallelFetcher.fetchAll(List.of(), id -> calls.incrementAndGet(), 2);

                assertThat(results).isEmpty();
                assertThat(calls).hasValue(0);
            }

            @Test
            @DisplayName("fetchAll works with more threads than ids")
            void moreThreadsThanIds() throws InterruptedException {
                assertThat(ParallelFetcher.fetchAll(List.of("1", "2"), Integer::valueOf, 8)).containsExactly(1, 2);
            }

            @Test
            @DisplayName("fetchAll returns a list that can't be modified")
            void returnsUnmodifiableList() throws InterruptedException {
                List<Integer> results = ParallelFetcher.fetchAll(List.of("1", "2"), Integer::valueOf, 2);

                assertThatThrownBy(() -> results.add(3)).isInstanceOf(UnsupportedOperationException.class);
            }

            @Test
            @DisplayName("fetchAll shuts its pool down before it returns")
            void shutsPoolDown() throws InterruptedException {
                Set<Thread> poolThreads = ConcurrentHashMap.newKeySet();

                ParallelFetcher.fetchAll(List.of("a", "b", "c"), id -> {
                    poolThreads.add(Thread.currentThread());
                    return 1;
                }, 2);

                assertAllStop(poolThreads);
            }

            @Test
            @DisplayName("fetchAll shuts its pool down after a failure, too")
            void shutsPoolDownAfterFailure() throws InterruptedException {
                Set<Thread> poolThreads = ConcurrentHashMap.newKeySet();

                assertThatThrownBy(() -> ParallelFetcher.fetchAll(List.of("a", "b", "c"), id -> {
                    poolThreads.add(Thread.currentThread());
                    if (id.equals("b")) {
                        throw new IllegalStateException("b is broken");
                    }
                    return 1;
                }, 2)).isInstanceOf(FetchException.class);

                assertAllStop(poolThreads);
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("fetchAll throws a FetchException whose cause is the exception the fetcher threw")
            void failureKeepsOriginalCause() {
                var failure = new IllegalStateException("service unavailable");

                assertThatThrownBy(() -> ParallelFetcher.fetchAll(List.of("a", "b", "c"), id -> {
                    if (id.equals("b")) {
                        throw failure;
                    }
                    return 1;
                }, 2)).isInstanceOfSatisfying(FetchException.class, e -> {
                    assertThat(e.id()).isEqualTo("b");
                    assertThat(e.getCause()).isSameAs(failure);
                });
            }

            @Test
            @DisplayName("fetchAll reports the first failing id in the order of the ids, not the first failure in time")
            void reportsFirstFailingIdInInputOrder() {
                Function<String, Integer> fetcher = id -> switch (id) {
                    case "b" -> {
                        Blocking.sleep(Duration.ofMillis(100)); // b fails later than d...
                        throw new IllegalStateException("b failed");
                    }
                    case "d" -> throw new IllegalStateException("d failed"); // ...which fails at once
                    default -> 1;
                };

                assertThatThrownBy(() -> ParallelFetcher.fetchAll(List.of("a", "b", "c", "d"), fetcher, 4))
                        .isInstanceOfSatisfying(FetchException.class, e -> assertThat(e.id()).isEqualTo("b"));
            }

            @Test
            @DisplayName("fetchAll cancels the remaining calls as soon as it knows it will fail")
            void cancelsRemainingCalls() {
                var started = new AtomicInteger();
                var interrupted = new AtomicInteger();
                var never = new CountDownLatch(1);
                List<String> ids = Stream.concat(Stream.of("broken"),
                        IntStream.range(0, 10).mapToObj(i -> "slow-" + i)).toList();
                Function<String, Integer> fetcher = id -> {
                    if (id.equals("broken")) {
                        throw new IllegalStateException("broken");
                    }
                    started.incrementAndGet();
                    try {
                        never.await(); // a slow call: it only ends when it is interrupted
                    } catch (InterruptedException e) {
                        interrupted.incrementAndGet();
                        Thread.currentThread().interrupt();
                    }
                    return 1;
                };

                assertTimeoutPreemptively(Blocking.PATIENCE, () ->
                        assertThatThrownBy(() -> ParallelFetcher.fetchAll(ids, fetcher, 2))
                                .isInstanceOf(FetchException.class));

                assertThat(started.get()).as("slow calls that started").isLessThanOrEqualTo(2);
                assertThat(interrupted).as("slow calls that were interrupted").hasValue(started.get());
            }

            @ParameterizedTest(name = "threads = {0}")
            @ValueSource(ints = {0, -1})
            @DisplayName("fetchAll rejects fewer than 1 thread")
            void rejectsThreadsBelowOne(int threads) {
                assertThatThrownBy(() -> ParallelFetcher.fetchAll(List.of("1"), Integer::valueOf, threads))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("fetchAll rejects a null list, a null id or a null fetcher before it calls anything")
            void rejectsNulls() {
                var calls = new AtomicInteger();
                Function<String, Integer> fetcher = id -> calls.incrementAndGet();

                assertThatThrownBy(() -> ParallelFetcher.fetchAll(null, fetcher, 2))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> ParallelFetcher.fetchAll(Arrays.asList("a", null), fetcher, 2))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> ParallelFetcher.fetchAll(List.of("a"), null, 2))
                        .isInstanceOf(NullPointerException.class);
                assertThat(calls).as("calls of the fetcher").hasValue(0);
            }
        }
    }
}
