package practice.j08.ex07;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import practice.j08.support.Blocking;
import practice.testkit.Concurrently;

/**
 * Note: JUnit runs every test method in its own thread, so each test starts outside of any request.
 */
@DisplayName("J08-07 · Virtual threads and scoped values")
class VirtualThreadsAndScopedValuesTest {

    @Nested
    @DisplayName("BlockingTasks.runAll")
    class RunAll {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("runAll calls the task once for every index")
            void callsTaskOncePerIndex() {
                Map<Integer, Integer> calls = new ConcurrentHashMap<>();

                BlockingTasks.runAll(100, index -> calls.merge(index, 1, Integer::sum));

                assertThat(calls).hasSize(100).allSatisfy((index, count) -> assertThat(count).isOne());
                assertThat(calls.keySet()).allSatisfy(index -> assertThat(index).isBetween(0, 99));
            }

            @Test
            @DisplayName("runAll runs every task on its own virtual thread")
            void runsEachTaskOnItsOwnVirtualThread() {
                Set<Thread> threads = ConcurrentHashMap.newKeySet();
                var platformThreads = new AtomicInteger();

                BlockingTasks.runAll(1_000, index -> {
                    Thread current = Thread.currentThread();
                    threads.add(current);
                    if (!current.isVirtual()) {
                        platformThreads.incrementAndGet();
                    }
                });

                assertThat(platformThreads).as("tasks that ran on a platform thread").hasValue(0);
                assertThat(threads).as("different threads").hasSize(1_000);
            }

            @Test
            @DisplayName("runAll returns only after every task has finished")
            void waitsForEveryTask() {
                var finished = new AtomicInteger();

                BlockingTasks.runAll(50, index -> {
                    Blocking.sleep(Duration.ofMillis(100));
                    finished.incrementAndGet();
                });

                assertThat(finished).hasValue(50);
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("runAll runs the tasks at the same time")
            void runsTasksAtTheSameTime() {
                var barrier = new CyclicBarrier(100);
                var metTheOthers = new AtomicInteger();

                BlockingTasks.runAll(100, index -> {
                    if (Blocking.allArrive(barrier)) {
                        metTheOthers.incrementAndGet();
                    }
                });

                assertThat(metTheOthers).as("tasks that were running together with all the others").hasValue(100);
            }

            @Test
            @DisplayName("runAll finishes 10,000 tasks that each block for 50 ms in well under 3 seconds")
            void manyBlockingTasksFinishQuickly() {
                long start = System.nanoTime();

                int failures = assertTimeoutPreemptively(Duration.ofSeconds(5),
                        () -> BlockingTasks.runAll(10_000, index -> Blocking.sleep(Duration.ofMillis(50))));

                Duration elapsed = Duration.ofNanos(System.nanoTime() - start);
                assertThat(failures).isZero();
                assertThat(elapsed).isLessThan(Duration.ofSeconds(3));
            }

            @Test
            @DisplayName("runAll counts the tasks that throw, and still runs all the others")
            void countsFailures() {
                var ran = new AtomicInteger();

                int failures = BlockingTasks.runAll(10, index -> {
                    ran.incrementAndGet();
                    if (index % 2 == 0) {
                        throw new IllegalStateException("task " + index + " fails on purpose");
                    }
                });

                assertThat(failures).isEqualTo(5);
                assertThat(ran).hasValue(10);
            }

            @Test
            @DisplayName("runAll with a count of 0 runs nothing and returns 0")
            void countZero() {
                var ran = new AtomicInteger();

                assertThat(BlockingTasks.runAll(0, index -> ran.incrementAndGet())).isZero();
                assertThat(ran).hasValue(0);
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("runAll rejects a negative count")
            void rejectsNegativeCount() {
                assertThatThrownBy(() -> BlockingTasks.runAll(-1, index -> { }))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("runAll rejects a null task")
            void rejectsNullTask() {
                assertThatThrownBy(() -> BlockingTasks.runAll(1, null)).isInstanceOf(NullPointerException.class);
            }
        }
    }

    @Nested
    @DisplayName("RequestContext")
    class Context {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("currentRequestId() returns the id inside runWithRequestId")
            void idIsCurrentInside() {
                List<Optional<String>> seen = new ArrayList<>();

                RequestContext.runWithRequestId("req-1", () -> seen.add(RequestContext.currentRequestId()));

                assertThat(seen).containsExactly(Optional.of("req-1"));
            }

            @Test
            @DisplayName("callWithRequestId returns the result of the action, which sees the id")
            void callReturnsResult() {
                String result = RequestContext.callWithRequestId("req-2",
                        () -> "handled " + RequestContext.currentRequestId().orElse("nothing"));

                assertThat(result).isEqualTo("handled req-2");
            }

            @Test
            @DisplayName("currentRequestId() is empty outside of a request")
            void emptyOutside() {
                assertThat(RequestContext.currentRequestId()).isEmpty();
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("a nested call sees its own id, and the outer id is current again after it")
            void nestedCalls() {
                List<Optional<String>> seen = new ArrayList<>();

                RequestContext.runWithRequestId("outer", () -> {
                    seen.add(RequestContext.currentRequestId());
                    RequestContext.runWithRequestId("inner", () -> seen.add(RequestContext.currentRequestId()));
                    seen.add(RequestContext.currentRequestId());
                });

                assertThat(seen).containsExactly(Optional.of("outer"), Optional.of("inner"), Optional.of("outer"));
            }

            @Test
            @DisplayName("the id is gone after the action has returned")
            void goneAfterReturn() {
                RequestContext.runWithRequestId("req-1", () -> { });
                RequestContext.callWithRequestId("req-2", () -> "done");

                assertThat(RequestContext.currentRequestId()).isEmpty();
            }

            @Test
            @DisplayName("the id is gone after the action has thrown, and the exception reaches the caller")
            void goneAfterThrow() {
                assertThatThrownBy(() -> RequestContext.runWithRequestId("req-1", () -> {
                    throw new IllegalStateException("request failed");
                })).isInstanceOf(IllegalStateException.class).hasMessage("request failed");

                assertThat(RequestContext.currentRequestId()).isEmpty();
            }

            @Test
            @DisplayName("a thread started inside the action doesn't see the id")
            void notVisibleInNewThread() {
                var seenByNewThread = new CompletableFuture<Optional<String>>();

                RequestContext.runWithRequestId("req-1", () -> Thread.ofPlatform().start(() -> {
                    try {
                        seenByNewThread.complete(RequestContext.currentRequestId());
                    } catch (Throwable e) {
                        seenByNewThread.completeExceptionally(e);
                    }
                }));

                assertThat(seenByNewThread).succeedsWithin(Blocking.PATIENCE).isEqualTo(Optional.empty());
            }

            @Test
            @DisplayName("a pooled thread doesn't keep the id for its next task")
            void noLeakIntoNextTaskOfPooledThread() throws Exception {
                try (ExecutorService pool = Executors.newSingleThreadExecutor()) {
                    pool.submit(() -> RequestContext.runWithRequestId("req-1", () -> { })).get();

                    Optional<String> seenByNextTask = pool.submit(RequestContext::currentRequestId).get();

                    assertThat(seenByNextTask).isEmpty();
                }
            }
        }

        @Nested
        @DisplayName("Concurrency")
        class Concurrency {

            @Test
            @DisplayName("8 threads handling requests at the same time each see only their own id")
            void eachThreadSeesItsOwnId() {
                int threads = 8;
                var everyoneHasAnId = new CyclicBarrier(threads);
                var wrong = new ConcurrentLinkedQueue<String>();

                Concurrently.run(threads, thread -> {
                    String id = "req-" + thread;
                    RequestContext.runWithRequestId(id, () -> {
                        Blocking.allArrive(everyoneHasAnId); // now every thread is inside its own request
                        for (int i = 0; i < 1_000; i++) {
                            String seen = RequestContext.currentRequestId().orElse("nothing");
                            if (!seen.equals(id)) {
                                wrong.add(id + " saw " + seen);
                                return;
                            }
                        }
                    });
                });

                assertThat(wrong).as("threads that saw another request's id").isEmpty();
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @ParameterizedTest(name = "id {0}")
            @ValueSource(strings = {"", " ", "\t"})
            @DisplayName("rejects a blank request id without running the action")
            void rejectsBlankId(String id) {
                var ran = new AtomicBoolean();

                assertThatThrownBy(() -> RequestContext.runWithRequestId(id, () -> ran.set(true)))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThatThrownBy(() -> RequestContext.callWithRequestId(id, () -> ran.getAndSet(true)))
                        .isInstanceOf(IllegalArgumentException.class);
                assertThat(ran).as("the action ran").isFalse();
            }

            @Test
            @DisplayName("rejects a null request id or action without running anything")
            void rejectsNulls() {
                var ran = new AtomicBoolean();

                assertThatThrownBy(() -> RequestContext.runWithRequestId(null, () -> ran.set(true)))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> RequestContext.callWithRequestId(null, () -> ran.getAndSet(true)))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> RequestContext.runWithRequestId("req-1", null))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> RequestContext.callWithRequestId("req-1", null))
                        .isInstanceOf(NullPointerException.class);
                assertThat(ran).as("the action ran").isFalse();
            }
        }
    }
}
