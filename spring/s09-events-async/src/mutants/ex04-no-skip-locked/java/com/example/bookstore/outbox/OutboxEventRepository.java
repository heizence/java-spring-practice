package com.example.bookstore.outbox;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Mutant support: adds a plain query without locks next to the locking one. */
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Transactional(propagation = Propagation.MANDATORY)
    @Query(value = """
            SELECT * FROM outbox_events
            WHERE published_at IS NULL
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED""", nativeQuery = true)
    List<OutboxEvent> lockUnpublished(int limit);

    List<OutboxEvent> findByPublishedAtIsNullOrderById(Limit limit);
}
