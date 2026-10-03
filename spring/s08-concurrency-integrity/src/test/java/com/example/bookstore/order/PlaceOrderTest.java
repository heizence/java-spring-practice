package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.Responses;
import com.example.bookstore.support.SeedData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Placing orders takes copies out of stock, without losing updates when many orders arrive at once. Book 1 has 10
 * copies in stock, books 59 and 60 have none ({@code spring/BOOKSTORE.md}).
 *
 * <p>Every request commits, as in production, and each test puts the seed data back afterwards.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S08-01 · Lost updates")
class PlaceOrderTest {

    @Autowired
    private MockMvcTester mvc;

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
        @DisplayName("an order takes its copies out of stock")
        void takesCopiesOutOfStock() {
            MvcTestResult result = placeOrder(1, 2);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(stockOf(1)).isEqualTo(8);
        }

        @Test
        @DisplayName("an order can take the last copies of a book")
        void takesTheLastCopies() {
            MvcTestResult result = placeOrder(1, 10);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(stockOf(1)).isZero();
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("every order increases the stock's version, so older copies of the inventory count as stale")
        void increasesTheVersion() {
            long versionBefore = versionOf(1);

            placeOrder(1, 1);
            placeOrder(1, 1);

            assertThat(versionOf(1)).isEqualTo(versionBefore + 2);
        }

        @Test
        @DisplayName("an order of two books takes copies of both")
        void takesCopiesOfEveryBook() {
            int book2Before = stockOf(2);

            MvcTestResult result = placeOrder("""
                    { "memberId": 1, "lines": [ { "bookId": 1, "quantity": 3 }, { "bookId": 2, "quantity": 1 } ] }""");

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(stockOf(1)).isEqualTo(7);
            assertThat(stockOf(2)).isEqualTo(book2Before - 1);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an order for more copies than are left fails with 409 OUT_OF_STOCK and changes nothing")
        void rejectsMoreThanTheStock() {
            long versionBefore = versionOf(1);

            MvcTestResult result = placeOrder(1, 11);

            assertThat(result)
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Only 10 copies of book 1 are left, but 11 were requested.",
                              "instance": "/api/orders",
                              "code": "OUT_OF_STOCK"
                            }""");
            assertThat(stockOf(1)).isEqualTo(10);
            assertThat(versionOf(1)).isEqualTo(versionBefore);
        }

        @Test
        @DisplayName("an order for a book that is out of stock fails with 409 OUT_OF_STOCK")
        void rejectsABookThatIsOutOfStock() {
            MvcTestResult result = placeOrder(59, 1);

            assertThat(Responses.outcome(result)).isEqualTo("409 OUT_OF_STOCK");
            assertThat(stockOf(59)).isZero();
        }

        @Test
        @DisplayName("when one book of an order is out of stock, the stock of the other books doesn't change either")
        void changesNothingWhenOneBookIsMissing() {
            MvcTestResult result = placeOrder("""
                    { "memberId": 1, "lines": [ { "bookId": 1, "quantity": 2 }, { "bookId": 59, "quantity": 1 } ] }""");

            assertThat(Responses.outcome(result)).isEqualTo("409 OUT_OF_STOCK");
            assertThat(stockOf(1)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("40 orders for one copy each, sent at the same moment, sell exactly the 10 copies in stock")
        void sellsExactlyTheStock() {
            long lastOrderBefore = lastOrderId();

            Responses responses = Responses.sendAtOnce(40, i -> placeOrder(1, 1));

            assertThat(responses.countByOutcome()).isEqualTo(Map.of("201", 10L, "409 OUT_OF_STOCK", 30L));
            assertThat(stockOf(1)).isZero();
            assertThat(copiesOrderedSince(lastOrderBefore, 1)).as("copies of book 1 in the new orders").isEqualTo(10);
        }

        @Test
        @DisplayName("orders for 1 to 3 copies, sent at the same moment, never take more copies than there were")
        void neverSellsMoreThanTheStock() {
            long lastOrderBefore = lastOrderId();

            Responses responses = Responses.sendAtOnce(30, i -> placeOrder(1, 1 + i % 3));

            int left = stockOf(1);
            long sold = copiesOrderedSince(lastOrderBefore, 1);
            assertThat(left).as("copies left").isNotNegative();
            assertThat(sold + left).as("copies sold (%d) plus copies left (%d)", sold, left).isEqualTo(10);
            assertThat(responses.countByOutcome()).containsOnlyKeys("201", "409 OUT_OF_STOCK");
            assertThat(ordersSince(lastOrderBefore)).as("new orders").isEqualTo(responses.withStatus(201).size());
        }
    }

    private MvcTestResult placeOrder(long bookId, int quantity) {
        return placeOrder("""
                { "memberId": 1, "lines": [ { "bookId": %d, "quantity": %d } ] }""".formatted(bookId, quantity));
    }

    private MvcTestResult placeOrder(String json) {
        return mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private int stockOf(long bookId) {
        return jdbc.sql("SELECT quantity FROM inventories WHERE book_id = ?").param(bookId)
                .query(Integer.class).single();
    }

    private long versionOf(long bookId) {
        return jdbc.sql("SELECT version FROM inventories WHERE book_id = ?").param(bookId)
                .query(Long.class).single();
    }

    private long lastOrderId() {
        return jdbc.sql("SELECT MAX(id) FROM orders").query(Long.class).single();
    }

    private long ordersSince(long lastOrderBefore) {
        return jdbc.sql("SELECT COUNT(*) FROM orders WHERE id > ?").param(lastOrderBefore)
                .query(Long.class).single();
    }

    private long copiesOrderedSince(long lastOrderBefore, long bookId) {
        return jdbc.sql("SELECT COALESCE(SUM(quantity), 0) FROM order_lines WHERE order_id > ? AND book_id = ?")
                .param(lastOrderBefore).param(bookId)
                .query(Long.class).single();
    }
}
