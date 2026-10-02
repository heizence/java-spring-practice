package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Comparator;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SqlStatementCounter;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * An example of a full integration test (lesson section 6), not part of an exercise: the whole application and the
 * real database, without mocks. It checks what the two slices can't: that the controller, the service, the
 * repositories and MySQL work together, including a side effect (the stock) and the number of SQL statements.
 */
@IntegrationTest
@DisplayName("Example · Orders end to end")
class OrderApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Test
    @DisplayName("placing an order saves it, takes the copies out of stock, and returns it")
    void placesAnOrder() {
        MvcTestResult placed = placeOrder("""
                { "memberId": 1, "lines": [ { "bookId": 1, "quantity": 2 } ] }""");

        assertThat(placed)
                .hasStatus(HttpStatus.CREATED)
                .bodyJson().isLenientlyEqualTo("""
                        { "memberId": 1, "status": "PENDING", "totalAmount": 72000, "orderedAt": "2026-10-15T00:00:00Z",
                          "lines": [ { "bookId": 1, "title": "Effective Java", "quantity": 2, "unitPrice": 36000,
                                       "amount": 72000 } ] }""");
        // The test's transaction never commits, so Hibernate hasn't written the new stock yet. Flush before reading
        // the database with plain SQL.
        entityManager.flush();
        assertThat(stockOf(1L)).as("copies of book 1 left").isEqualTo(10 - 2);
        String location = placed.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(mvc.get().uri(location))
                .hasStatusOk()
                .bodyJson().extractingPath("$.totalAmount").asNumber().isEqualTo(72_000);
    }

    @Test
    @DisplayName("an order with a book that's out of stock is rejected with 409 OUT_OF_STOCK and not saved")
    void rejectsAnOrderThatCantBeFilled() {
        long ordersBefore = countOrders();

        MvcTestResult rejected = placeOrder("""
                { "memberId": 1, "lines": [ { "bookId": 1, "quantity": 1 }, { "bookId": 59, "quantity": 1 } ] }""");

        assertThat(rejected)
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().extractingPath("$.code").isEqualTo("OUT_OF_STOCK");
        assertThat(countOrders()).isEqualTo(ordersBefore);
    }

    @Test
    @DisplayName("lists a member's orders newest first, with three SQL statements")
    void listsAMembersOrders() {
        MvcTestResult page = mvc.get().uri("/api/members/1/orders?size=5").exchange();

        assertThat(page)
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.page.totalElements", total -> assertThat(total).asNumber().isEqualTo(14))
                .hasPathSatisfying("$.content[*].orderedAt", times -> assertThat(times).asArray()
                        .hasSize(5)
                        .extracting(time -> Instant.parse((String) time))
                        .isSortedAccordingTo(Comparator.reverseOrder()));
        // Does the member exist? The page of orders. The count for the page's total. No query per order.
        assertThat(sqlStatements.count()).as("%s", sqlStatements).isEqualTo(3);
    }

    private MvcTestResult placeOrder(String json) {
        return mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private int stockOf(long bookId) {
        return jdbc.sql("SELECT quantity FROM inventories WHERE book_id = :bookId")
                .param("bookId", bookId).query(Integer.class).single();
    }

    private long countOrders() {
        return jdbc.sql("SELECT COUNT(*) FROM orders").query(Long.class).single();
    }
}
