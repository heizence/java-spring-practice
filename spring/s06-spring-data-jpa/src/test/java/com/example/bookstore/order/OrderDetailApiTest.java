package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.orm.jpa.support.OpenEntityManagerInViewInterceptor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /api/orders/{orderId}}, as a client calls it. These tests turn the test transaction off: it would keep a
 * persistence context open around the whole request, so lazy loading would work in the controller and hide the bug.
 * They only read, so there is nothing to clean up.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S06-06 · Lazy loading outside a transaction")
class OrderDetailApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private ApplicationContext applicationContext;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("GET /api/orders/{id} returns the order with its lines and the books' titles")
        void returnsAnOrder() {
            long orderId = jdbc.sql("SELECT MIN(id) FROM orders WHERE member_id = 1").query(Long.class).single();

            assertThat(mvc.get().uri("/api/orders/{id}", orderId))
                    .hasStatusOk()
                    .bodyJson().convertTo(OrderResponse.class)
                    .isEqualTo(orderFromTheDatabase(orderId));
        }

        @Test
        @DisplayName("returns an order with three lines, each with its own book")
        void returnsAnOrderWithThreeLines() {
            long orderId = jdbc.sql("""
                    SELECT order_id FROM order_lines GROUP BY order_id HAVING COUNT(*) = 3
                    ORDER BY order_id LIMIT 1""").query(Long.class).single();

            assertThat(mvc.get().uri("/api/orders/{id}", orderId))
                    .hasStatusOk()
                    .bodyJson().convertTo(OrderResponse.class)
                    .isEqualTo(orderFromTheDatabase(orderId));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("keeps spring.jpa.open-in-view off: the fix belongs in the service, not in the web layer")
        void openInViewStaysOff() {
            assertThat(applicationContext.getBeanNamesForType(OpenEntityManagerInViewInterceptor.class))
                    .as("beans that keep a persistence context open for a whole web request")
                    .isEmpty();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("answers 404 ORDER_NOT_FOUND for an order that doesn't exist")
        void unknownOrder() {
            assertThat(mvc.get().uri("/api/orders/99999"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Order 99999 was not found.",
                              "instance": "/api/orders/99999",
                              "code": "ORDER_NOT_FOUND"
                            }""");
        }
    }

    /** The order and its lines, read with plain SQL. */
    private OrderResponse orderFromTheDatabase(long orderId) {
        List<OrderLineResponse> lines = jdbc.sql("""
                        SELECT l.book_id, b.title, l.quantity, l.unit_price
                        FROM order_lines l JOIN books b ON b.id = l.book_id
                        WHERE l.order_id = ? ORDER BY l.id""")
                .param(orderId)
                .query((row, rowNumber) -> new OrderLineResponse(row.getLong("book_id"), row.getString("title"),
                        row.getInt("quantity"), row.getLong("unit_price"),
                        row.getInt("quantity") * row.getLong("unit_price")))
                .list();
        return jdbc.sql("SELECT id, member_id, status, total_amount, ordered_at FROM orders WHERE id = ?")
                .param(orderId)
                .query((row, rowNumber) -> new OrderResponse(row.getLong("id"), row.getLong("member_id"),
                        OrderStatus.valueOf(row.getString("status")), row.getLong("total_amount"),
                        row.getTimestamp("ordered_at").toInstant(), lines))
                .single();
    }
}
