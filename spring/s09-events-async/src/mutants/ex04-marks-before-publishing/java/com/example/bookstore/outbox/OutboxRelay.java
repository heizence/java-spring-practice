package com.example.bookstore.outbox;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/** Mutant: marks each event published before publishing it, so an event that fails is never tried again. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final MessagePublisher messagePublisher;
    private final Clock clock;

    @Transactional
    public int publishPending(int batchSize) {
        Assert.isTrue(batchSize >= 1, () -> "batchSize must be at least 1: " + batchSize);
        int published = 0;
        for (OutboxEvent event : outboxEventRepository.lockUnpublished(batchSize)) {
            event.markPublished(clock.instant());
            try {
                messagePublisher.publish(new Message(event.getId(), event.getEventType(), event.getAggregateId(),
                        event.getPayload()));
                published++;
            } catch (RuntimeException e) {
                event.recordFailedAttempt();
                log.warn("Could not publish outbox event {}", event.getId(), e);
            }
        }
        return published;
    }
}
