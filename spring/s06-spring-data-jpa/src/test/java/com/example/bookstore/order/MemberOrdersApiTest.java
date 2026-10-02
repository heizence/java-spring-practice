package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SqlStatementCounter;
import com.example.bookstore.support.TestClock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * A member's order history, {@code GET /api/members/{memberId}/orders}: the right orders and lines, and few SQL
 * statements. The expected orders come from plain SQL over the seed data (Minji, member 1, has 14 orders).
 */
@IntegrationTest
@DisplayName("S06-05 · The N+1 problem")
class MemberOrdersApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private MemberRepository members;

    @Autowired
    private TestClock clock;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("lists a member's orders newest first, with their lines and the books' titles")
        void listsTheOrders() {
            assertThat(mvc.get().uri("/api/members/1/orders?size=20"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.content").convertTo(list(OrderResponse.class))
                    .containsExactlyElementsOf(minjisOrdersNewestFirst());
        }

        @Test
        @DisplayName("returns the first 10 orders by default, with the page's metadata")
        void firstPage() {
            assertThat(mvc.get().uri("/api/members/1/orders"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.page", page -> assertThat(page).asMap().containsExactlyInAnyOrderEntriesOf(
                            Map.of("size", 10, "number", 0, "totalElements", 14, "totalPages", 2)))
                    .extractingPath("$.content").convertTo(list(OrderResponse.class))
                    .containsExactlyElementsOf(minjisOrdersNewestFirst().subList(0, 10));
        }
    }

    @Nested
    @DisplayName("Queries")
    class Queries {

        @Test
        @DisplayName("loads a page of 10 orders with their lines and books in at most 5 SQL statements")
        void noQueryPerOrder() {
            assertThat(mvc.get().uri("/api/members/1/orders")).hasStatusOk();

            assertThat(sqlStatements.count()).as("%s", sqlStatements).isLessThanOrEqualTo(5);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the second page holds the 4 oldest orders")
        void secondPage() {
            assertThat(mvc.get().uri("/api/members/1/orders?page=1"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.content").convertTo(list(OrderResponse.class))
                    .containsExactlyElementsOf(minjisOrdersNewestFirst().subList(10, 14));
        }

        @Test
        @DisplayName("lists an order whose lines were all removed, with no lines")
        void orderWithoutLines() {
            Order empty = orders.save(Order.place(members.getReferenceById(1L), clock.instant()));
            entityManager.flush();
            entityManager.clear();
            List<OrderResponse> expected = minjisOrdersNewestFirst();
            assertThat(expected.getFirst()).as("the new order, the newest of Minji's 15")
                    .isEqualTo(new OrderResponse(empty.getId(), 1L, OrderStatus.PENDING, 0, TestClock.DEFAULT_INSTANT,
                            List.of()));

            assertThat(mvc.get().uri("/api/members/1/orders?size=20"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.page.totalElements", total -> assertThat(total).asNumber().isEqualTo(15))
                    .extractingPath("$.content").convertTo(list(OrderResponse.class))
                    .containsExactlyElementsOf(expected);
        }

        @Test
        @DisplayName("a member without orders gets an empty page")
        void memberWithoutOrders() {
            Member newMember = members.save(Member.register("no.orders@example.com", "No Orders", "{noop}secret"));

            assertThat(mvc.get().uri("/api/members/{id}/orders", newMember.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content", content -> assertThat(content).asArray().isEmpty())
                    .hasPathSatisfying("$.page.totalElements", total -> assertThat(total).asNumber().isEqualTo(0));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("answers 404 MEMBER_NOT_FOUND for a member that doesn't exist")
        void unknownMember() {
            assertThat(mvc.get().uri("/api/members/999/orders"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Member 999 was not found.",
                              "instance": "/api/members/999/orders",
                              "code": "MEMBER_NOT_FOUND"
                            }""");
        }
    }

    /** Minji's orders with their lines, read with plain SQL: newest first, lines in the order they were added. */
    private List<OrderResponse> minjisOrdersNewestFirst() {
        Map<Long, List<OrderLineResponse>> linesByOrder = jdbc.sql("""
                        SELECT l.order_id, l.book_id, b.title, l.quantity, l.unit_price
                        FROM order_lines l JOIN books b ON b.id = l.book_id JOIN orders o ON o.id = l.order_id
                        WHERE o.member_id = 1
                        ORDER BY l.id""")
                .query((row, rowNumber) -> Map.entry(row.getLong("order_id"), new OrderLineResponse(
                        row.getLong("book_id"), row.getString("title"), row.getInt("quantity"),
                        row.getLong("unit_price"), row.getInt("quantity") * row.getLong("unit_price"))))
                .stream()
                .collect(Collectors.groupingBy(Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        return jdbc.sql("""
                        SELECT id, status, total_amount, ordered_at FROM orders
                        WHERE member_id = 1
                        ORDER BY ordered_at DESC, id DESC""")
                .query((row, rowNumber) -> new OrderResponse(row.getLong("id"), 1L,
                        OrderStatus.valueOf(row.getString("status")), row.getLong("total_amount"),
                        row.getTimestamp("ordered_at").toInstant(),
                        linesByOrder.getOrDefault(row.getLong("id"), List.of())))
                .list();
    }
}
