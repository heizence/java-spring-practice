package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.config.ScheduledTaskHolder;

/**
 * {@link PendingOrderCleanupService#cancelExpiredOrders()}, called directly with the test clock, the way the
 * scheduled job calls it.
 *
 * <p>The tests run in the usual rolled-back test transaction, which the service joins. Before reading the tables with
 * plain SQL, they flush Hibernate's pending changes. The seeded orders are from August and September 2026; with the
 * clock at 2026-08-01, none of them is old enough, so a test can work with orders of its own.
 */
@IntegrationTest
@DisplayName("S09-03 · Scheduled jobs")
class PendingOrderCleanupServiceTest {

    private static final Duration MAX_AGE = Duration.ofHours(72);              // application.yml
    private static final Instant AUGUST_1 = Instant.parse("2026-08-01T00:00:00Z"); // before every seeded order
    private static final long MINJI = 1;
    private static final long EFFECTIVE_JAVA = 1; // 10 copies in stock

    @Autowired
    private PendingOrderCleanupService cleanupService;

    @Autowired
    private TestClock clock;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationContext context;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("cancels every PENDING order older than the maximum age: all 33 seeded ones")
        void cancelsTheSeededPendingOrders() {
            int cancelled = cleanupService.cancelExpiredOrders();
            entityManager.flush();

            assertThat(cancelled).isEqualTo(33);
            assertThat(ordersByStatus()).doesNotContainKey("PENDING").containsEntry("CANCELLED", 11L + 33);
        }

        @Test
        @DisplayName("puts the copies of a cancelled order back into stock")
        void returnsTheCopies() {
            clock.setInstant(AUGUST_1);
            long orderId = pendingOrderOfMinji(EFFECTIVE_JAVA, 2, AUGUST_1.minus(Duration.ofDays(4)));

            int cancelled = cleanupService.cancelExpiredOrders();
            entityManager.flush();

            assertThat(cancelled).isEqualTo(1);
            assertThat(statusOf(orderId)).isEqualTo("CANCELLED");
            assertThat(stockOf(EFFECTIVE_JAVA)).isEqualTo(10);
        }

        @Test
        @DisplayName("keeps a PENDING order that is younger than the maximum age")
        void keepsAYoungOrder() {
            clock.setInstant(AUGUST_1);
            long orderId = pendingOrderOfMinji(EFFECTIVE_JAVA, 1, AUGUST_1.minus(Duration.ofDays(1)));

            int cancelled = cleanupService.cancelExpiredOrders();
            entityManager.flush();

            assertThat(cancelled).isZero();
            assertThat(statusOf(orderId)).isEqualTo("PENDING");
            assertThat(stockOf(EFFECTIVE_JAVA)).isEqualTo(9);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("an order exactly the maximum age old stays; one a microsecond older is cancelled")
        void exactlyTheMaximumAge() {
            clock.setInstant(AUGUST_1);
            long exactlyMaxAge = pendingOrderOfMinji(EFFECTIVE_JAVA, 1, AUGUST_1.minus(MAX_AGE));
            long aMicrosecondOlder = pendingOrderOfMinji(EFFECTIVE_JAVA, 1,
                    AUGUST_1.minus(MAX_AGE).minusNanos(1_000));

            int cancelled = cleanupService.cancelExpiredOrders();
            entityManager.flush();

            assertThat(cancelled).isEqualTo(1);
            assertThat(statusOf(exactlyMaxAge)).isEqualTo("PENDING");
            assertThat(statusOf(aMicrosecondOlder)).isEqualTo("CANCELLED");
        }

        @Test
        @DisplayName("takes 'now' from the application's Clock: on 2026-08-01, no seeded order is old enough")
        void usesTheApplicationClock() {
            clock.setInstant(AUGUST_1);

            int cancelled = cleanupService.cancelExpiredOrders();
            entityManager.flush();

            assertThat(cancelled).isZero();
            assertThat(ordersByStatus()).containsEntry("PENDING", 33L);
        }

        @Test
        @DisplayName("never touches PAID, SHIPPED or CANCELLED orders, however old they are")
        void leavesOtherStatusesAlone() {
            cleanupService.cancelExpiredOrders();
            entityManager.flush();

            assertThat(ordersByStatus()).containsEntry("PAID", 108L).containsEntry("SHIPPED", 48L);
        }

        @Test
        @DisplayName("a second run finds nothing more to cancel")
        void secondRun() {
            cleanupService.cancelExpiredOrders();

            int cancelledAgain = cleanupService.cancelExpiredOrders();

            assertThat(cancelledAgain).isZero();
        }
    }

    @Nested
    @DisplayName("Scheduling")
    class Scheduling {

        @Test
        @DisplayName("the integration tests' application context schedules nothing (bookstore.scheduling.enabled=false)")
        void nothingIsScheduledInTests() {
            assertThat(context.getBeanProvider(ScheduledTaskHolder.class).stream()
                    .flatMap(holder -> holder.getScheduledTasks().stream()))
                    .isEmpty();
        }
    }

    /** Saves a PENDING order of Minji's and takes its copies out of stock, as placing an order does. */
    private long pendingOrderOfMinji(long bookId, int copies, Instant orderedAt) {
        Order order = Order.place(memberRepository.getReferenceById(MINJI), orderedAt);
        order.addLine(bookRepository.findById(bookId).orElseThrow(), copies);
        inventoryRepository.findById(bookId).orElseThrow().decrease(copies);
        return orderRepository.save(order).getId();
    }

    private Map<String, Long> ordersByStatus() {
        return jdbc.sql("SELECT status, COUNT(*) AS orders FROM orders GROUP BY status")
                .query((row, rowNumber) -> Map.entry(row.getString("status"), row.getLong("orders")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private String statusOf(long orderId) {
        return jdbc.sql("SELECT status FROM orders WHERE id = ?").param(orderId).query(String.class).single();
    }

    private int stockOf(long bookId) {
        return jdbc.sql("SELECT quantity FROM inventories WHERE book_id = ?").param(bookId)
                .query(Integer.class).single();
    }
}
