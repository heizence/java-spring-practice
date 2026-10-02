package com.example.bookstore.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** These tests commit their changes, the way concurrency tests must, and put the seed data back afterwards. */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("Template · SeedData")
class SeedDataTest {

    @Autowired
    private SeedData seedData;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Test
    @DisplayName("restore() undoes committed changes, and ids start again after the seed data")
    void restoresCommittedChanges() {
        jdbc.sql("UPDATE inventories SET quantity = 0 WHERE book_id = 1").update();
        jdbc.sql("DELETE FROM reviews WHERE book_id = 1").update();
        jdbc.sql("INSERT INTO categories (name) VALUES ('Poetry')").update();

        seedData.restore();

        assertThat(jdbc.sql("SELECT quantity FROM inventories WHERE book_id = 1").query(Integer.class).single())
                .isEqualTo(10);
        assertThat(jdbc.sql("SELECT COUNT(*) FROM reviews WHERE book_id = 1").query(Long.class).single())
                .isEqualTo(15);
        assertThat(jdbc.sql("SELECT name FROM categories ORDER BY id").query(String.class).list())
                .containsExactly("Programming", "Databases", "Software Architecture", "Fiction", "Science",
                        "Self-Help");
        jdbc.sql("INSERT INTO categories (name) VALUES ('Poetry')").update();
        assertThat(jdbc.sql("SELECT id FROM categories WHERE name = 'Poetry'").query(Long.class).single())
                .isEqualTo(7);
    }

    @Test
    @DisplayName("restore() refuses to run inside a transaction")
    void refusesToRunInsideATransaction() {
        transactionTemplate.executeWithoutResult(status ->
                assertThatThrownBy(seedData::restore)
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("Propagation.NOT_SUPPORTED"));
    }
}
