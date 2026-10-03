package com.example.bookstore.common;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/**
 * Copies the caller's MDC to the thread that runs a task, so that the task's log lines carry the caller's request
 * id.
 *
 * <p>The MDC (mapped diagnostic context) is a map of values, such as {@code requestId}, that the logger adds to every
 * line. It belongs to one thread, and pool threads run one task after another, so:
 * <ul>
 *   <li>{@link #decorate} runs on the caller's thread, when the task is handed to the executor: take a copy of the
 *       caller's MDC there;</li>
 *   <li>the task it returns runs on a pool thread: it makes that copy the thread's MDC (an empty MDC if the caller
 *       had none), runs the original task, and afterwards always clears the MDC, even when the task throws, so that
 *       nothing leaks into the next task on that thread.</li>
 * </ul>
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        // Still on the caller's thread: this is the last moment its MDC can be read.
        Map<String, String> callersContext = MDC.getCopyOfContextMap();
        return () -> {
            // Now on a pool thread, which may still hold the MDC of the task it ran before.
            if (callersContext == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(callersContext);
            }
            try {
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
