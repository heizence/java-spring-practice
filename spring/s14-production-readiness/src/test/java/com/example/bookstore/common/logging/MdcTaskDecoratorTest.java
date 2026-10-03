package com.example.bookstore.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * {@link MdcTaskDecorator} without Spring: each test decorates a task on the test thread and runs it on a new
 * thread, which plays the part of a thread of the task executor's pool.
 */
@DisplayName("S14-01 · Logging with request IDs")
class MdcTaskDecoratorTest {

    private final MdcTaskDecorator decorator = new MdcTaskDecorator();

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("the task sees the MDC of the thread that submitted it")
        void taskSeesTheSubmittersMdc() throws InterruptedException {
            MDC.put("requestId", "checkout-7f3a");
            var seen = new AtomicReference<String>();
            Runnable task = decorator.decorate(() -> seen.set(MDC.get("requestId")));

            runOnAPoolThread(Map.of(), task);

            assertThat(seen).hasValue("checkout-7f3a");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("copies the MDC when the task is submitted: later changes on the submitting thread don't count")
        void copiesAtSubmitTime() throws InterruptedException {
            MDC.put("requestId", "first");
            var seen = new AtomicReference<String>();
            Runnable task = decorator.decorate(() -> seen.set(MDC.get("requestId")));
            MDC.put("requestId", "second");

            runOnAPoolThread(Map.of(), task);

            assertThat(seen).hasValue("first");
        }

        @Test
        @DisplayName("a task submitted with an empty MDC runs with an empty MDC, on any thread")
        void emptyMdcStaysEmpty() throws InterruptedException {
            var seen = new AtomicReference<Map<String, String>>(Map.of("not", "run"));
            Runnable task = decorator.decorate(() -> seen.set(copyOfMdc()));

            runOnAPoolThread(Map.of("requestId", "left-over"), task);

            assertThat(seen.get()).isEmpty();
        }

        @Test
        @DisplayName("gives the pool thread its own MDC back after the task")
        void restoresThePoolThreadsMdc() throws InterruptedException {
            MDC.put("requestId", "checkout-7f3a");
            Runnable task = decorator.decorate(() -> { });

            Map<String, String> after = runOnAPoolThread(Map.of("requestId", "pool"), task);

            assertThat(after).containsExactly(Map.entry("requestId", "pool"));
        }

        @Test
        @DisplayName("leaves a pool thread without MDC entries empty after the task")
        void leavesAnEmptyPoolThreadEmpty() throws InterruptedException {
            MDC.put("requestId", "checkout-7f3a");
            Runnable task = decorator.decorate(() -> { });

            Map<String, String> after = runOnAPoolThread(Map.of(), task);

            assertThat(after).isEmpty();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("gives the pool thread its own MDC back also when the task throws")
        void restoresAfterAnException() throws InterruptedException {
            MDC.put("requestId", "checkout-7f3a");
            Runnable task = decorator.decorate(() -> {
                throw new IllegalStateException("the mail server is down");
            });

            Map<String, String> after = runOnAPoolThread(Map.of("requestId", "pool"), task);

            assertThat(after).containsExactly(Map.entry("requestId", "pool"));
        }
    }

    /**
     * Runs a task on a new thread whose MDC starts with {@code poolThreadMdc}, like a pool thread that ran other tasks
     * before. Exceptions from the task are swallowed, as an executor would report them elsewhere.
     *
     * @return the thread's MDC after the task
     */
    private static Map<String, String> runOnAPoolThread(Map<String, String> poolThreadMdc, Runnable task)
            throws InterruptedException {
        var after = new AtomicReference<Map<String, String>>();
        Thread thread = Thread.ofPlatform().start(() -> {
            MDC.setContextMap(poolThreadMdc);
            try {
                task.run();
            } catch (RuntimeException expected) {
                // The test that throws on purpose checks what happens afterwards.
            } finally {
                after.set(copyOfMdc());
            }
        });
        assertThat(thread.join(Duration.ofSeconds(5))).as("the task finished").isTrue();
        return after.get();
    }

    private static Map<String, String> copyOfMdc() {
        Map<String, String> copy = MDC.getCopyOfContextMap();
        return copy == null ? Map.of() : new HashMap<>(copy);
    }
}
