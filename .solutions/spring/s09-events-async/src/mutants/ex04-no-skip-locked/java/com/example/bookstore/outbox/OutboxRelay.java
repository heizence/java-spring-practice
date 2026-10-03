package com.example.bookstore.outbox;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/** Mutant: reads the unpublished events without locking them, so two relays publish the same events. */
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
        for (OutboxEvent event : outboxEventRepository.findByPublishedAtIsNullOrderById(Limit.of(batchSize))) {
            try {
                messagePublisher.publish(new Message(event.getId(), event.getEventType(), event.getAggregateId(),
                        event.getPayload()));
                event.markPublished(clock.instant());
                published++;
            } catch (RuntimeException e) {
                event.recordFailedAttempt();
                log.warn("Could not publish outbox event {}", event.getId(), e);
            }
        }
        return published;
    }
}
