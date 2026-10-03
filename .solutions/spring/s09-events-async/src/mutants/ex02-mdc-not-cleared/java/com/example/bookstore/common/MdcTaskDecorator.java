package com.example.bookstore.common;

import java.util.Map;

import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

/** Mutant: copies the caller's MDC, but never clears it, so a pool thread keeps it for its next task. */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        Map<String, String> callersContext = MDC.getCopyOfContextMap();
        return () -> {
            if (callersContext != null) {
                MDC.setContextMap(callersContext);
            }
            runnable.run();
        };
    }
}
