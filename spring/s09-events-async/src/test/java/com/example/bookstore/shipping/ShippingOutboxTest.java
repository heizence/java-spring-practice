package com.example.bookstore.shipping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
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
import tools.jackson.databind.json.JsonMapper;

/**
 * {@link ShippingService#ship} writes its event to the outbox, in the same transaction as the order.
 *
 * <p>No test transaction ({@code NOT_SUPPORTED}): the tests check what is committed. Each test puts the seed data
 * back at the end.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S09-04 · The transactional outbox")
class ShippingOutboxTest {

    @Autowired
    private ShippingService shippingService;

    @Autowired
    private FakeMessagePublisher publisher;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private JsonMapper jsonMapper;

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
        @DisplayName("ships a paid order and records an unpublished OrderShipped event in the outbox")
        void recordsTheEvent() {
            long orderId = firstOrderWithStatus("PAID");
            long memberId = jdbc.sql("SELECT member_id FROM orders WHERE id = ?").param(orderId)
                    .query(Long.class).single();

            shippingService.ship(orderId);

            assertThat(statusOf(orderId)).isEqualTo("SHIPPED");
            List<Map<String, Object>> events = outboxEvents();
            assertThat(events).hasSize(1);
            Map<String, Object> event = events.getFirst();
            assertThat(event).containsEntry("aggregate_type", "Order").containsEntry("aggregate_id", orderId)
                    .containsEntry("event_type", "OrderShipped").containsEntry("published_at", null)
                    .containsEntry("attempts", 0).containsEntry("created_at", TestClock.DEFAULT_INSTANT);
            assertThat(jsonMapper.readValue((String) event.get("payload"), OrderShipped.class))
                    .isEqualTo(new OrderShipped(orderId, memberId, TestClock.DEFAULT_INSTANT));
        }

        @Test
        @DisplayName("writes the event in the order's transaction: when it rolls back, both are gone")
        void rollsBackTogether() {
            long orderId = firstOrderWithStatus("PAID");

            transactionTemplate.executeWithoutResult(status -> {
                shippingService.ship(orderId);
                status.setRollbackOnly(); // the caller's transaction, which ship() joined, ends in a rollback
            });

            assertThat(statusOf(orderId)).isEqualTo("PAID");
            assertThat(outboxEvents()).isEmpty();
        }

        @Test
        @DisplayName("doesn't call the message broker itself: the relay publishes later")
        void doesNotPublish() {
            shippingService.ship(firstOrderWithStatus("PAID"));

            assertThat(publisher.published()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a PENDING order isn't shipped, and no event is recorded")
        void pendingOrder() {
            long orderId = firstOrderWithStatus("PENDING");

            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> shippingService.ship(orderId))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
            assertThat(statusOf(orderId)).isEqualTo("PENDING");
            assertThat(outboxEvents()).isEmpty();
        }

        @Test
        @DisplayName("an unknown order is rejected with ORDER_NOT_FOUND, and no event is recorded")
        void unknownOrder() {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> shippingService.ship(9_999))
                    .extracting(BusinessException::getErrorCode)
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
            assertThat(outboxEvents()).isEmpty();
        }
    }

    private long firstOrderWithStatus(String status) {
        return jdbc.sql("SELECT MIN(id) FROM orders WHERE status = ?").param(status).query(Long.class).single();
    }

    private String statusOf(long orderId) {
        return jdbc.sql("SELECT status FROM orders WHERE id = ?").param(orderId).query(String.class).single();
    }

    /** Every outbox row, oldest first, with its times as Instants. */
    private List<Map<String, Object>> outboxEvents() {
        return jdbc.sql("""
                        SELECT aggregate_type, aggregate_id, event_type, payload, created_at, published_at, attempts
                        FROM outbox_events ORDER BY id""")
                .query((row, rowNumber) -> {
                    Map<String, Object> event = new HashMap<>();
                    event.put("aggregate_type", row.getString("aggregate_type"));
                    event.put("aggregate_id", row.getLong("aggregate_id"));
                    event.put("event_type", row.getString("event_type"));
                    event.put("payload", row.getString("payload"));
                    event.put("created_at", toInstant(row.getTimestamp("created_at")));
                    event.put("published_at", toInstant(row.getTimestamp("published_at")));
                    event.put("attempts", row.getInt("attempts"));
                    return event;
                })
                .list();
    }

    private static Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }
}
