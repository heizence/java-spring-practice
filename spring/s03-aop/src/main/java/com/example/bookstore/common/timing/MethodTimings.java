package com.example.bookstore.common.timing;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Component;

/**
 * The most recent {@link MethodTiming}s, oldest first. An operator can look at them to find slow operations; in S14,
 * Micrometer metrics take over this job.
 *
 * <p>The bean is shared by every request thread, so its methods are {@code synchronized}. It keeps at most
 * {@value #CAPACITY} timings and forgets the oldest ones, so that it can't fill the memory of a long-running
 * application.
 */
@Component
public class MethodTimings {

    static final int CAPACITY = 1_000;

    private final Deque<MethodTiming> recent = new ArrayDeque<>();

    public synchronized void record(MethodTiming timing) {
        Objects.requireNonNull(timing, "timing");
        if (recent.size() == CAPACITY) {
            recent.removeFirst();
        }
        recent.addLast(timing);
    }

    /** A snapshot of the recorded timings, oldest first. */
    public synchronized List<MethodTiming> all() {
        return List.copyOf(recent);
    }

    /** Forgets every timing. */
    public synchronized void clear() {
        recent.clear();
    }
}
