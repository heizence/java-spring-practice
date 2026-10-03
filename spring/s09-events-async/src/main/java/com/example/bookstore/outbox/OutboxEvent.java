package com.example.bookstore.outbox;

import java.time.Instant;
import java.util.Optional;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

/**
 * An event waiting in the transactional outbox ({@code outbox_events}) until {@link OutboxRelay} publishes it.
 * Save it in the same transaction as the business change it describes.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "outbox_events")
@EntityListeners(AuditingEntityListener.class)
public class OutboxEvent {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** What the event is about, such as {@code Order}. */
    private String aggregateType;

    /** The id of the thing the event is about, such as the order's id. */
    private Long aggregateId;

    /** Such as {@code OrderShipped}. */
    private String eventType;

    /** The event as JSON. MySQL stores it in a {@code JSON} column, which may reorder the properties. */
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    /** Filled by Spring Data auditing from the {@code Clock} when the event is saved. */
    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    /** {@code null} until the event is published; read it through {@link #getPublishedAt()}. */
    private Instant publishedAt;

    /** How many times publishing this event has failed. */
    private int attempts;

    private OutboxEvent(String aggregateType, long aggregateId, String eventType, String payload) {
        Assert.hasText(aggregateType, "aggregateType must not be blank");
        Assert.hasText(eventType, "eventType must not be blank");
        Assert.hasText(payload, "payload must not be blank");
        this.aggregateType = aggregateType;
        this.aggregateId = aggregateId;
        this.eventType = eventType;
        this.payload = payload;
    }

    /**
     * A new, unpublished event.
     *
     * @param payload the event as JSON
     */
    public static OutboxEvent create(String aggregateType, long aggregateId, String eventType, String payload) {
        return new OutboxEvent(aggregateType, aggregateId, eventType, payload);
    }

    /** When the event was published, or empty while it is waiting. */
    public Optional<Instant> getPublishedAt() {
        return Optional.ofNullable(publishedAt);
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    /**
     * Records that the message broker has the event.
     *
     * @throws IllegalStateException if the event was published before
     */
    public void markPublished(Instant now) {
        Assert.notNull(now, "now must not be null");
        Assert.state(publishedAt == null, () -> "Outbox event %d was already published at %s".formatted(id,
                publishedAt));
        this.publishedAt = now;
    }

    /** Records a failed attempt to publish the event. It stays unpublished, so the relay tries it again. */
    public void recordFailedAttempt() {
        attempts++;
    }
}
