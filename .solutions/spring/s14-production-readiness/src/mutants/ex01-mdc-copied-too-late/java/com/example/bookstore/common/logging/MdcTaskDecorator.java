package com.example.bookstore.common.logging;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;
import org.springframework.stereotype.Component;

/**
 * Carries the MDC, and with it the request id, from the thread that hands a task to the application's task executor
 * (for example by calling an {@code @Async} method) to the thread that runs the task.
 *
 * <p>The MDC belongs to one thread, so a task on another thread starts without the request id. Spring Boot gives the
 * executor that runs {@code @Async} methods every {@link TaskDecorator} bean, which wraps each task before a thread
 * runs it:
 *
 * <ul>
 *   <li>{@link #decorate} copies the MDC of the thread that calls it, at that moment.</li>
 *   <li>While the task runs, the worker thread's MDC is exactly that copy. With an empty MDC at decoration time,
 *       the task runs with an empty MDC.</li>
 *   <li>Afterwards, the worker thread's MDC is what it was before the task, also when the task throws. Pool threads
 *       run many tasks, and one task's request id must not leak into the next task's log lines.</li>
 * </ul>
 */
@Component
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable task) {
        return () -> {
            Map<String, String> submitted = MDC.getCopyOfContextMap();
            Map<String, String> previous = MDC.getCopyOfContextMap();
            replaceMdc(submitted);
            try {
                task.run();
            } finally {
                replaceMdc(previous);
            }
        };
    }

    private static void replaceMdc(Map<String, String> entries) {
        if (entries == null) {
            MDC.clear();
        } else {
            MDC.setContextMap(entries);
        }
    }
}
