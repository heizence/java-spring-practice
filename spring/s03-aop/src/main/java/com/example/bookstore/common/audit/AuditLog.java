package com.example.bookstore.common.audit;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The audit log: a record of the operations that change something important, kept so that someone can later find out
 * what happened ("Who got coupon FLASH10000, and when?").
 *
 * <p>Each entry is written to the application log under the logger name {@code audit}, where the operations team
 * collects it, and the most recent {@value #CAPACITY} entries are also kept in memory, oldest first. (A real
 * application would also store entries in a database table. S03 has no database.) The bean is shared by every
 * request thread, so its methods are {@code synchronized}.
 */
@Component
public class AuditLog {

    static final int CAPACITY = 1_000;

    private static final Logger AUDIT = LoggerFactory.getLogger("audit");

    private final Deque<AuditEntry> recent = new ArrayDeque<>();

    public synchronized void add(AuditEntry entry) {
        Objects.requireNonNull(entry, "entry");
        AUDIT.info("{}", entry);
        if (recent.size() == CAPACITY) {
            recent.removeFirst();
        }
        recent.addLast(entry);
    }

    /** A snapshot of the entries in memory, oldest first. */
    public synchronized List<AuditEntry> entries() {
        return List.copyOf(recent);
    }

    /** Forgets the entries in memory. (The log file keeps them.) */
    public synchronized void clear() {
        recent.clear();
    }
}
