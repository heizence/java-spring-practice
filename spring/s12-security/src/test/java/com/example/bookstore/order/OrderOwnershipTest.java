package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@code GET /api/orders/{id}}: a member reads only their own orders. The seed data has 200 orders, 14 of them
 * Minji's; the tests look the ids up instead of relying on undocumented rows.
 */
@IntegrationTest
@DisplayName("S12-05 · Authorization rules")
class OrderOwnershipTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a member reads their own order, with its lines")
        void readsOwnOrder() {
            long orderId = firstOrderOf(TestUsers.MINJI);
            long lines = jdbc.sql("SELECT COUNT(*) FROM order_lines WHERE order_id = :id").param("id", orderId)
                    .query(Long.class).single();

            assertThat(mvc.get().uri("/api/orders/{id}", orderId).with(TestUsers.minji()))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.id", id -> assertThat(id).asNumber().isEqualTo((int) orderId))
                    .hasPathSatisfying("$.lines", orderLines -> assertThat(orderLines).asArray().hasSize((int) lines));
        }

        @Test
        @DisplayName("another member's order is 404 ORDER_NOT_FOUND")
        void anotherMembersOrderIsNotFound() {
            long othersOrder = firstOrderNotOf(TestUsers.MINJI);

            assertThat(mvc.get().uri("/api/orders/{id}", othersOrder).with(TestUsers.minji()))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Order %d was not found.",
                              "instance": "/api/orders/%d",
                              "code": "ORDER_NOT_FOUND"
                            }""".formatted(othersOrder, othersOrder));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("every one of Minji's 14 orders is readable for her")
        void allOwnOrdersAreReadable() {
            var minjisOrders = jdbc.sql("SELECT id FROM orders WHERE member_id = :member ORDER BY id")
                    .param("member", TestUsers.MINJI).query(Long.class).list();

            assertThat(minjisOrders).hasSize(14).allSatisfy(orderId ->
                    assertThat(mvc.get().uri("/api/orders/{id}", orderId).with(TestUsers.minji())).hasStatusOk());
        }

        @Test
        @DisplayName("no order of another member is readable for Minji")
        void noOtherOrderIsReadable() {
            var othersOrders = jdbc.sql("SELECT id FROM orders WHERE member_id <> :member ORDER BY id")
                    .param("member", TestUsers.MINJI).query(Long.class).list();

            assertThat(othersOrders).hasSize(186).allSatisfy(orderId ->
                    assertThat(mvc.get().uri("/api/orders/{id}", orderId).with(TestUsers.minji()))
                            .hasStatus(HttpStatus.NOT_FOUND));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("another member's order looks exactly like an order that doesn't exist")
        void ownershipDoesNotLeak() {
            long othersOrder = firstOrderNotOf(TestUsers.MINJI);
            long missing = 9999;

            assertThat(mvc.get().uri("/api/orders/{id}", othersOrder).with(TestUsers.minji()))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.detail").isEqualTo("Order %d was not found.".formatted(othersOrder));
            assertThat(mvc.get().uri("/api/orders/{id}", missing).with(TestUsers.minji()))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.detail").isEqualTo("Order %d was not found.".formatted(missing));
        }

        @Test
        @DisplayName("an administrator gets 404 for another member's order on this endpoint too")
        void adminHasNoOrdersHere() {
            long minjisOrder = firstOrderOf(TestUsers.MINJI);

            assertThat(mvc.get().uri("/api/orders/{id}", minjisOrder).with(TestUsers.admin()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    private long firstOrderOf(long memberId) {
        return jdbc.sql("SELECT MIN(id) FROM orders WHERE member_id = :member").param("member", memberId)
                .query(Long.class).single();
    }

    private long firstOrderNotOf(long memberId) {
        return jdbc.sql("SELECT MIN(id) FROM orders WHERE member_id <> :member").param("member", memberId)
                .query(Long.class).single();
    }
}
