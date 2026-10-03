package com.example.bookstore.outbox;

/**
 * Sends messages to the message broker, such as Kafka or RabbitMQ: a remote call that can fail. This course uses no
 * real broker; see {@code LoggingMessagePublisher}.
 */
public interface MessagePublisher {

    /**
     * Returns once the broker has stored the message.
     *
     * @throws RuntimeException if the broker can't be reached or refuses the message
     */
    void publish(Message message);
}
