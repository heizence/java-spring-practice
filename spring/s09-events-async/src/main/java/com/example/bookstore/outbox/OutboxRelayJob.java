package com.example.bookstore.outbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls the outbox: runs the {@link OutboxRelay} every {@code bookstore.outbox.relay-interval}, when scheduling is
 * switched on (S09-03).
 */
@Slf4j
@Component
@RequiredArgsConstructor
@EnableConfigurationProperties(OutboxProperties.class)
class OutboxRelayJob {

    private final OutboxRelay relay;
    private final OutboxProperties properties;

    @Scheduled(fixedDelayString = "${bookstore.outbox.relay-interval}",
            initialDelayString = "${bookstore.outbox.relay-interval}")
    void run() {
        int published = relay.publishPending(properties.batchSize());
        if (published > 0) {
            log.info("Published {} outbox events", published);
        }
    }
}
