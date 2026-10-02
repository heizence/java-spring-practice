package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.TestClock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Placing orders and removing their lines, end to end. These tests run without the test transaction: each request
 * commits or rolls back on its own, as in production, so they also show whether the service's transaction is right.
 * The seed data is put back after each test.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S06-03 · Relationships and aggregates")
class OrderApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedData seedData;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private TestClock clock;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("POST /api/orders answers 201 Created with the new PENDING order and its Location")
        void placesAnOrder() {
            BookFacts book2 = book(2);

            MvcTestResult result = placeOrder(1, line(1, 2), line(2, 1));

            long id = idOf(result);
            assertThat(result)
                    .hasStatus(HttpStatus.CREATED)
                    .headers().hasValue(HttpHeaders.LOCATION, "/api/orders/" + id);
            assertThat(result).bodyJson().isStrictlyEqualTo("""
                    {
                      "id": %d,
                      "memberId": 1,
                      "status": "PENDING",
                      "totalAmount": %d,
                      "orderedAt": "2026-10-15T00:00:00Z",
                      "lines": [
                        { "bookId": 1, "title": "Effective Java", "quantity": 2, "unitPrice": 36000, "amount": 72000 },
                        { "bookId": 2, "title": "%s", "quantity": 1, "unitPrice": %d, "amount": %d }
                      ]
                    }""".formatted(id, 72_000 + book2.price(), book2.title(), book2.price(), book2.price()));
        }

        @Test
        @DisplayName("saves the order and every line in the database")
        void savesTheOrderAndItsLines() {
            long price2 = book(2).price();

            long id = idOf(placeOrder(1, line(1, 2), line(2, 1)));

            assertThat(orderRow(id))
                    .containsEntry("member_id", 1L)
                    .containsEntry("status", "PENDING")
                    .containsEntry("total_amount", 72_000 + price2);
            assertThat(linesInDatabase(id)).containsExactly("book 1 x2 at 36000", "book 2 x1 at " + price2);
        }

        @Test
        @DisplayName("takes the ordered copies out of stock")
        void decreasesTheStock() {
            int stockOfBook2 = book(2).stock();

            assertThat(placeOrder(1, line(1, 2), line(2, 1))).hasStatus(HttpStatus.CREATED);

            assertThat(stock(1)).isEqualTo(10 - 2);
            assertThat(stock(2)).isEqualTo(stockOfBook2 - 1);
        }

        @Test
        @DisplayName("saving an order with the repository saves its new lines too (cascade)")
        void cascadesTheSave() {
            Long id = transactionTemplate.execute(status -> {
                Order order = Order.place(memberRepository.getReferenceById(1L), clock.instant());
                order.addLine(bookRepository.getReferenceById(1L), 3);
                return orderRepository.save(order).getId();
            });

            assertThat(linesInDatabase(id)).containsExactly("book 1 x3 at 36000");
        }

        @Test
        @DisplayName("a line removed from a loaded order is deleted from the database (orphan removal)")
        void deletesOrphanLines() {
            long id = saveOrderWithBooks(1, 2);

            transactionTemplate.executeWithoutResult(status -> orderRepository.findById(id).orElseThrow()
                    .removeLine(bookRepository.getReferenceById(2L)));

            assertThat(linesInDatabase(id)).containsExactly("book 1 x1 at 36000");
        }

        @Test
        @DisplayName("DELETE /api/orders/{id}/lines/{bookId} answers 204 No Content and deletes the line")
        void removesALine() {
            long id = idOf(placeOrder(1, line(1, 2), line(2, 1)));

            assertThat(mvc.delete().uri("/api/orders/{id}/lines/2", id)).hasStatus(HttpStatus.NO_CONTENT);

            assertThat(linesInDatabase(id)).containsExactly("book 1 x2 at 36000");
        }

        @Test
        @DisplayName("removing a line lowers the order's total and puts the copies back into stock")
        void removingALineUpdatesTotalAndStock() {
            long price2 = book(2).price();
            long id = idOf(placeOrder(1, line(1, 2), line(2, 1)));

            assertThat(mvc.delete().uri("/api/orders/{id}/lines/1", id)).hasStatus(HttpStatus.NO_CONTENT);

            assertThat(orderRow(id)).containsEntry("total_amount", price2);
            assertThat(stock(1)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the same book twice in one request becomes one line with both quantities")
        void mergesTheSameBook() {
            MvcTestResult result = placeOrder(1, line(1, 2), line(1, 3));

            assertThat(result).hasStatus(HttpStatus.CREATED)
                    .bodyJson().hasPathSatisfying("$.lines[*].quantity",
                            quantities -> assertThat(quantities).asArray().containsExactly(5));
            assertThat(linesInDatabase(idOf(result))).containsExactly("book 1 x5 at 36000");
            assertThat(stock(1)).isEqualTo(5);
        }

        @Test
        @DisplayName("an order may take the last copies of a book")
        void takesTheLastCopies() {
            assertThat(placeOrder(1, line(1, 10))).hasStatus(HttpStatus.CREATED);

            assertThat(stock(1)).isZero();
        }

        @Test
        @DisplayName("removing the last line leaves an empty PENDING order with a total of 0")
        void removesTheLastLine() {
            long id = idOf(placeOrder(1, line(1, 1)));

            assertThat(mvc.delete().uri("/api/orders/{id}/lines/1", id)).hasStatus(HttpStatus.NO_CONTENT);

            assertThat(linesInDatabase(id)).isEmpty();
            assertThat(orderRow(id)).containsEntry("status", "PENDING").containsEntry("total_amount", 0L);
            assertThat(stock(1)).isEqualTo(10);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("answers 409 OUT_OF_STOCK and saves nothing, not even the other lines' stock changes")
        void outOfStock() {
            assertThat(placeOrder(1, line(1, 2), line(59, 1)))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Only 0 copies of book 59 are left, but 1 were requested.",
                              "instance": "/api/orders",
                              "code": "OUT_OF_STOCK"
                            }""");

            assertThat(orderCount()).isEqualTo(200);
            assertThat(stock(1)).isEqualTo(10);
        }

        @Test
        @DisplayName("answers 404 MEMBER_NOT_FOUND for a member that doesn't exist")
        void unknownMember() {
            assertThat(placeOrder(999, line(1, 1)))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("MEMBER_NOT_FOUND"))
                    .hasPathSatisfying("$.detail",
                            detail -> assertThat(detail).asString().isEqualTo("Member 999 was not found."));
        }

        @Test
        @DisplayName("answers 404 BOOK_NOT_FOUND for a book that doesn't exist, and saves nothing")
        void unknownBook() {
            assertThat(placeOrder(1, line(1, 1), line(9999, 1)))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("BOOK_NOT_FOUND"));

            assertThat(orderCount()).isEqualTo(200);
            assertThat(stock(1)).isEqualTo(10);
        }

        @Test
        @DisplayName("answers 400 INVALID_INPUT for an order without lines")
        void noLines() {
            assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "memberId": 1, "lines": [] }"""))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("INVALID_INPUT"));
        }

        @Test
        @DisplayName("answers 409 INVALID_ORDER_STATUS for removing a line of a paid order, and keeps the line")
        void lineOfAPaidOrder() {
            Map<String, Object> paidLine = jdbc.sql("""
                    SELECT o.id AS order_id, l.book_id FROM orders o JOIN order_lines l ON l.order_id = o.id
                    WHERE o.status = 'PAID' ORDER BY o.id, l.id LIMIT 1""").query().singleRow();
            long orderId = (Long) paidLine.get("order_id");
            long bookId = (Long) paidLine.get("book_id");
            List<String> lines = linesInDatabase(orderId);
            int stock = stock(bookId);

            assertThat(mvc.delete().uri("/api/orders/{orderId}/lines/{bookId}", orderId, bookId))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("INVALID_ORDER_STATUS"))
                    .hasPathSatisfying("$.detail", detail -> assertThat(detail).asString().isEqualTo(
                            "Order %d is PAID; its lines can only change while it is PENDING.".formatted(orderId)));

            assertThat(linesInDatabase(orderId)).isEqualTo(lines);
            assertThat(stock(bookId)).isEqualTo(stock);
        }

        @Test
        @DisplayName("answers 404 ORDER_LINE_NOT_FOUND for a book that isn't in the order")
        void bookNotInTheOrder() {
            long id = idOf(placeOrder(1, line(1, 1)));

            assertThat(mvc.delete().uri("/api/orders/{id}/lines/2", id))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("ORDER_LINE_NOT_FOUND"))
                    .hasPathSatisfying("$.detail", detail -> assertThat(detail).asString()
                            .isEqualTo("Order %d has no line for book 2.".formatted(id)));
        }

        @Test
        @DisplayName("answers 404 ORDER_NOT_FOUND for an order that doesn't exist")
        void unknownOrder() {
            assertThat(mvc.delete().uri("/api/orders/99999/lines/1"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("ORDER_NOT_FOUND"));
        }
    }

    /** A book's title, price and stock, read with plain SQL. */
    private record BookFacts(String title, long price, int stock) {
    }

    private BookFacts book(long id) {
        return jdbc.sql("""
                        SELECT b.title, b.price, i.quantity FROM books b JOIN inventories i ON i.book_id = b.id
                        WHERE b.id = ?""")
                .param(id)
                .query((row, rowNumber) -> new BookFacts(row.getString("title"), row.getLong("price"),
                        row.getInt("quantity")))
                .single();
    }

    private static String line(long bookId, int quantity) {
        return """
                { "bookId": %d, "quantity": %d }""".formatted(bookId, quantity);
    }

    private MvcTestResult placeOrder(long memberId, String... lines) {
        String body = """
                { "memberId": %d, "lines": [ %s ] }""".formatted(memberId, String.join(", ", lines));
        return mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    /** The id of the order a successful POST created, read from its response body. */
    private static long idOf(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        try {
            return JsonPath.parse(result.getResponse().getContentAsString()).read("$.id", Long.class);
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    /** Saves a new order for Minji with one copy of each book, without the API, and returns its id. */
    private long saveOrderWithBooks(long... bookIds) {
        return transactionTemplate.execute(status -> {
            Order order = Order.place(memberRepository.getReferenceById(1L), clock.instant());
            Arrays.stream(bookIds).forEach(bookId -> order.addLine(bookRepository.getReferenceById(bookId), 1));
            return orderRepository.save(order).getId();
        });
    }

    private Map<String, Object> orderRow(long id) {
        return jdbc.sql("SELECT member_id, status, total_amount FROM orders WHERE id = ?").param(id).query()
                .singleRow();
    }

    /** The order's lines as "book 1 x2 at 36000", in the order of their ids. */
    private List<String> linesInDatabase(long orderId) {
        return jdbc.sql("SELECT book_id, quantity, unit_price FROM order_lines WHERE order_id = ? ORDER BY id")
                .param(orderId)
                .query((row, rowNumber) -> "book %d x%d at %d".formatted(row.getLong("book_id"),
                        row.getInt("quantity"), row.getLong("unit_price")))
                .list();
    }

    private int stock(long bookId) {
        return jdbc.sql("SELECT quantity FROM inventories WHERE book_id = ?").param(bookId).query(Integer.class)
                .single();
    }

    private long orderCount() {
        return jdbc.sql("SELECT COUNT(*) FROM orders").query(Long.class).single();
    }
}
