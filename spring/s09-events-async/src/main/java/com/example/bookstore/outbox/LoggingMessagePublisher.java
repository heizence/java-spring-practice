package com.example.bookstore.outbox;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Stands in for a real message broker while you run the application locally: it only logs each message. Tests
 * replace it with a fake that records the messages.
 */
@Slf4j
@Component
class LoggingMessagePublisher implements MessagePublisher {

    @Override
    public void publish(Message message) {
        log.info("Published {} {} for key {}: {}", message.eventType(), message.eventId(), message.key(),
                message.payload());
    }
}
