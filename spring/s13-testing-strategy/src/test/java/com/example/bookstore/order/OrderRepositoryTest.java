package com.example.bookstore.order;

import com.example.bookstore.common.ClockConfiguration;
import com.example.bookstore.common.JpaAuditingConfiguration;
import com.example.bookstore.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;
import org.springframework.context.annotation.Import;

/**
 * S13-02 · Write tests: a repository slice.
 *
 * <p>Write tests that pin down the rules of the three custom queries in {@link OrderRepository}:
 * {@code findMemberOrders}, {@code totalSpentBy} and {@code findPendingOrderedBefore}, as their Javadoc describes
 * them. Your tests run against several broken versions of the repository; a broken version is <em>caught</em> when at
 * least one of your tests fails on it. Run {@code ./score s13-02} to see how many you catch.
 *
 * <p>This is a JPA slice: Spring starts JPA, the repositories and Flyway against the real MySQL in Testcontainers,
 * and nothing else. Each test runs in a transaction that is rolled back. The database also holds the seed data of
 * {@code spring/BOOKSTORE.md}, so don't assume that a table is empty: save the members and orders your test needs
 * (with {@link OrderBuilder}) and check what happens to them.
 *
 * <p>Structure your tests like the ones in the other exercises: {@code @Nested} classes for "Basic", "Edge cases" and
 * "Errors", a {@code @DisplayName} on every test, given/when/then inside. Don't open {@code src/mutants} until you've
 * caught them all: it's the answer key.
 */
@DataJpaTest
@Import({TestcontainersConfiguration.class, JpaAuditingConfiguration.class, ClockConfiguration.class})
@DisplayName("S13-02 · Write tests: a repository slice")
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orders;

    @Autowired
    private TestEntityManager entityManager;

    // TODO: S13-02 — write your tests here.
}
