package com.example.bookstore.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.example.bookstore.support.FakeMessagePublisher;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * {@link OutboxRelay#publishPending}: publishes, marks, retries, and shares the work with other relays.
 *
 * <p>No test transaction ({@code NOT_SUPPORTED}): the relay must commit, and the concurrency test runs two relays in
 * transactions of their own. The events are saved directly, so these tests don't depend on {@code ShippingService}.
 * {@link FakeMessagePublisher} records the messages, and can refuse an event or hold a call. Each test puts the seed
 * data back at the end.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S09-04 · The transactional outbox")
class OutboxRelayTest {

    @Autowired
    private OutboxRelay relay;

    @Autowired
    private FakeMessagePublisher publisher;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedData seedData;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("publishes the unpublished events, oldest first, and marks them published at the clock's time")
        void publishesAndMarks() {
            long first = savedEvent(201);
            long second = savedEvent(202);
            long third = savedEvent(203);

            int published = relay.publishPending(10);

            assertThat(published).isEqualTo(3);
            assertThat(publisher.published()).extracting(Message::eventId).containsExactly(first, second, third);
            assertThat(publishedAt(first)).isEqualTo(TestClock.DEFAULT_INSTANT);
            assertThat(publishedAt(second)).isEqualTo(TestClock.DEFAULT_INSTANT);
            assertThat(publishedAt(third)).isEqualTo(TestClock.DEFAULT_INSTANT);
        }

        @Test
        @DisplayName("each message carries the event's id, its type, the order's id as the key, and the payload")
        void messageContent() {
            long eventId = savedEvent(201);
            String storedPayload = jdbc.sql("SELECT payload FROM outbox_events WHERE id = ?").param(eventId)
                    .query(String.class).single();

            relay.publishPending(10);

            assertThat(publisher.published()).containsExactly(
                    new Message(eventId, "OrderShipped", 201, storedPayload));
        }

        @Test
        @DisplayName("a second run publishes nothing that was published already")
        void secondRun() {
            savedEvent(201);
            savedEvent(202);
            relay.publishPending(10);

            int publishedAgain = relay.publishPending(10);

            assertThat(publishedAgain).isZero();
            assertThat(publisher.published()).hasSize(2);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("publishes at most batchSize events per run, oldest first")
        void batchSize() {
            long first = savedEvent(201);
            long second = savedEvent(202);
            long third = savedEvent(203);

            int firstRun = relay.publishPending(2);
            int secondRun = relay.publishPending(2);

            assertThat(firstRun).isEqualTo(2);
            assertThat(secondRun).isEqualTo(1);
            assertThat(publisher.published()).extracting(Message::eventId).containsExactly(first, second, third);
        }

        @Test
        @DisplayName("an event that fails stays unpublished with one more attempt, and the next run publishes it")
        void retriesAFailedEvent() {
            long first = savedEvent(201);
            long failing = savedEvent(202);
            long third = savedEvent(203);
            publisher.refuse(failing);

            int firstRun = relay.publishPending(10);

            assertThat(firstRun).isEqualTo(2);
            assertThat(publisher.published()).extracting(Message::eventId).containsExactly(first, third);
            assertThat(publishedAt(failing)).isNull();
            assertThat(attemptsOf(failing)).isEqualTo(1);

            publisher.recover();
            int secondRun = relay.publishPending(10);

            assertThat(secondRun).isEqualTo(1);
            assertThat(publisher.published()).extracting(Message::eventId).containsExactly(first, third, failing);
            assertThat(publishedAt(failing)).isEqualTo(TestClock.DEFAULT_INSTANT);
            assertThat(attemptsOf(failing)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a batch size below 1")
        void rejectsBatchSizeBelowOne() {
            assertThatIllegalArgumentException().isThrownBy(() -> relay.publishPending(0));
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        /**
         * Relay A takes all three events and stops at its first publish call, which the fake broker holds. While A
         * holds its locks, relay B runs. Only then does the test release A.
         */
        @Test
        @DisplayName("two relays at the same time publish each event once: B skips the events that A has locked")
        void twoRelaysShareTheWork() throws Exception {
            long first = savedEvent(201);
            long second = savedEvent(202);
            long third = savedEvent(203);
            publisher.holdNextCall();

            ExecutorService threads = Executors.newFixedThreadPool(2, Thread.ofPlatform().daemon().factory());
            try {
                Future<Integer> relayA = threads.submit(() -> relay.publishPending(10));
                assertThat(publisher.awaitHeldCall(Duration.ofSeconds(5)))
                        .as("relay A is publishing its first event").isTrue();

                Future<Integer> relayB = threads.submit(() -> relay.publishPending(10));
                int publishedByB = resultWithin(relayB, Duration.ofSeconds(3),
                        "relay B waited for relay A's locks instead of skipping the locked events");
                publisher.releaseHeldCall();
                int publishedByA = resultWithin(relayA, Duration.ofSeconds(5), "relay A didn't finish");

                assertThat(publishedByB).as("events published by relay B").isZero();
                assertThat(publishedByA).as("events published by relay A").isEqualTo(3);
            } finally {
                publisher.releaseHeldCall();
                threads.shutdownNow();
                threads.awaitTermination(10, TimeUnit.SECONDS);
            }
            assertThat(publisher.published()).extracting(Message::eventId)
                    .as("each event exactly once").containsExactlyInAnyOrder(first, second, third);
        }
    }

    /** Saves (and commits) an unpublished OrderShipped event of an order. */
    private long savedEvent(long orderId) {
        Long eventId = transactionTemplate.execute(status -> outboxEventRepository.save(OutboxEvent.create(
                "Order", orderId, "OrderShipped", "{\"orderId\": %d}".formatted(orderId))).getId());
        assertThat(eventId).isNotNull();
        return eventId;
    }

    /** When the event was published, or {@code null} while it waits. */
    private Instant publishedAt(long eventId) {
        return jdbc.sql("SELECT published_at FROM outbox_events WHERE id = ?").param(eventId)
                .query((row, rowNumber) -> Optional.ofNullable(row.getTimestamp("published_at"))
                        .map(Timestamp::toInstant))
                .single()
                .orElse(null);
    }

    private int attemptsOf(long eventId) {
        return jdbc.sql("SELECT attempts FROM outbox_events WHERE id = ?").param(eventId)
                .query(Integer.class).single();
    }

    private static int resultWithin(Future<Integer> relay, Duration timeout, String messageIfLate)
            throws InterruptedException, ExecutionException {
        try {
            return relay.get(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            throw new AssertionError(messageIfLate, e);
        }
    }
}
