package com.example.bookstore.outbox;

/**
 * A message for the message broker.
 *
 * @param eventId   the outbox event's id. Delivery is "at least once": after a crash, the same event can be published
 *                  again, so a consumer remembers the ids it has handled and skips repeats.
 * @param eventType such as {@code OrderShipped}
 * @param key       the id of the thing the event is about. Brokers keep the messages of one key in order.
 * @param payload   the event as JSON
 */
public record Message(long eventId, String eventType, long key, String payload) {
}
