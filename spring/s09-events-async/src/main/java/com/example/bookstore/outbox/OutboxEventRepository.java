package com.example.bookstore.outbox;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Stores the events of the transactional outbox. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Locks and returns up to {@code limit} unpublished events, oldest first.
     *
     * <p>{@code FOR UPDATE} locks the rows until the caller's transaction ends, so the caller can publish them and
     * mark them published before anyone else may touch them. {@code SKIP LOCKED} makes another relay instance skip
     * the rows that this one has locked, instead of waiting for them: each instance takes the next free events.
     *
     * <p>{@code MANDATORY}: called without a transaction, the locks would end as soon as the query does, so it fails
     * at once with {@code IllegalTransactionStateException}.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<OutboxEvent> lockUnpublished(int limit);
}
