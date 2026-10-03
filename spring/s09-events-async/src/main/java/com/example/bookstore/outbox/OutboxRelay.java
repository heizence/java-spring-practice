package com.example.bookstore.outbox;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Publishes the events of the transactional outbox to the message broker. {@code OutboxRelayJob} runs it on a
 * schedule; tests call it directly.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventRepository outboxEventRepository;
    private final MessagePublisher messagePublisher;
    private final Clock clock;

    /**
     * Publishes up to {@code batchSize} unpublished events, oldest first.
     *
     * <ul>
     *   <li>Each event becomes one {@link Message}: the event's id, its type, its aggregate id as the key, and its
     *       payload.</li>
     *   <li>A published event is marked published, at "now" by the {@code Clock}, and is never published again.</li>
     *   <li>When publishing an event fails, the event stays unpublished, its attempts go up by one, and the next run
     *       tries it again. The other events of the batch are still published.</li>
     *   <li>Several instances of the application may run the relay at the same time, and each event is published
     *       by one of them only: use {@link OutboxEventRepository#lockUnpublished}, whose locks last until the
     *       relay's transaction ends.</li>
     * </ul>
     *
     * @param batchSize the most events to publish in this run; at least 1
     * @return how many events were published
     * @throws IllegalArgumentException if {@code batchSize} is less than 1
     */
    public int publishPending(int batchSize) {
        // TODO: S09-04
        throw new UnsupportedOperationException("TODO: S09-04");
    }
}
