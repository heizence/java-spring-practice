package com.example.bookstore.support;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.example.bookstore.order.Order;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

@IntegrationTest
@DisplayName("Template · SqlStatementCounter")
class SqlStatementCounterTest {

    private static final String TEN_ORDERS = "SELECT o FROM Order o WHERE o.id <= 10 ORDER BY o.id";

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("counts an N+1 pattern: one query for the orders, then one per order for its lines")
    void countsNPlusOne() {
        List<Order> orders = entityManager.createQuery(TEN_ORDERS, Order.class).getResultList();
        orders.forEach(order -> order.getLines().size()); // loads each order's lines with its own query

        assertThat(sqlStatements.count()).as("%s", sqlStatements).isEqualTo(1 + 10);
        assertThat(sqlStatements.statements().getFirst()).startsWith("select").contains("from orders");
        assertThat(sqlStatements.statements().subList(1, 11))
                .allSatisfy(sql -> assertThat(sql).contains("from order_lines"));
    }

    @Test
    @DisplayName("counts one statement when a fetch join loads the same data")
    void countsAFetchJoin() {
        List<Order> orders = entityManager.createQuery(
                "SELECT DISTINCT o FROM Order o JOIN FETCH o.lines WHERE o.id <= 10 ORDER BY o.id", Order.class)
                .getResultList();
        orders.forEach(order -> order.getLines().size());

        assertThat(sqlStatements.count()).as("%s", sqlStatements).isEqualTo(1);
    }

    @Test
    @DisplayName("starts each test empty, and reset() forgets earlier statements")
    void resets() {
        assertThat(sqlStatements.count()).isZero();
        entityManager.createQuery(TEN_ORDERS, Order.class).getResultList();
        assertThat(sqlStatements.count()).isEqualTo(1);

        sqlStatements.reset();

        assertThat(sqlStatements.count()).isZero();
        assertThat(sqlStatements.statements()).isEmpty();
    }

    @Test
    @DisplayName("doesn't count SQL that bypasses Hibernate")
    void ignoresPlainJdbc() {
        jdbc.sql("SELECT COUNT(*) FROM orders").query(Long.class).single();

        assertThat(sqlStatements.count()).isZero();
    }

    @Test
    @DisplayName("lists the statements, numbered, for assertion messages")
    void describesTheStatements() {
        entityManager.createQuery(TEN_ORDERS, Order.class).getResultList();

        assertThat(sqlStatements.toString())
                .startsWith("1 SQL statement(s)" + System.lineSeparator() + "1. select")
                .contains("from orders");
    }

    @Test
    @DisplayName("counts statements from several threads")
    void countsFromSeveralThreads() throws InterruptedException {
        var counter = new SqlStatementCounter();
        List<Thread> threads = List.of(
                Thread.ofVirtual().start(() -> recordStatements(counter)),
                Thread.ofVirtual().start(() -> recordStatements(counter)),
                Thread.ofVirtual().start(() -> recordStatements(counter)),
                Thread.ofVirtual().start(() -> recordStatements(counter)));

        for (Thread thread : threads) {
            thread.join();
        }

        assertThat(counter.count()).isEqualTo(4 * 1_000);
    }

    private static void recordStatements(SqlStatementCounter counter) {
        for (int i = 0; i < 1_000; i++) {
            counter.inspect("select 1");
        }
    }
}
