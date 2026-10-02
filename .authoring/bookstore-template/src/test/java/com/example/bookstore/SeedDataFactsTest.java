package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * The migrations create the schema and load exactly the seed data that {@code spring/BOOKSTORE.md} documents.
 * Checked with plain SQL, independently of the entities.
 */
@IntegrationTest
@DisplayName("Template · Flyway migrations and the documented seed data")
class SeedDataFactsTest {

    @Autowired
    private JdbcClient jdbc;

    @Test
    @DisplayName("Flyway applies V1 (schema) and V2 (seed data)")
    void appliesTheMigrations() {
        List<String> versions = jdbc.sql(
                        "SELECT version FROM flyway_schema_history WHERE success ORDER BY installed_rank")
                .query(String.class).list();

        assertThat(versions).containsExactly("1", "2");
    }

    @ParameterizedTest(name = "{0} has {1} rows", quoteTextArguments = false)
    @CsvSource({
            "categories, 6", "books, 60", "inventories, 60", "members, 20", "wallets, 20", "wallet_transactions, 175",
            "coupons, 4", "coupon_issues, 30", "orders, 200", "order_lines, 357", "payments, 156", "reviews, 300"})
    @DisplayName("each table has the documented number of rows")
    void rowCounts(String table, long expectedRows) {
        long rows = jdbc.sql("SELECT COUNT(*) FROM " + table).query(Long.class).single();

        assertThat(rows).isEqualTo(expectedRows);
    }

    @Test
    @DisplayName("book 1 is Effective Java at 36,000 won with 10 in stock; books 59 and 60 are out of stock")
    void books() {
        Map<String, Object> book1 = jdbc.sql("""
                SELECT b.title, b.author, b.price, i.quantity FROM books b JOIN inventories i ON i.book_id = b.id
                WHERE b.id = 1""").query().singleRow();
        List<Long> outOfStock = jdbc.sql("SELECT book_id FROM inventories WHERE quantity = 0 ORDER BY book_id")
                .query(Long.class).list();
        List<String> koreanTitles = jdbc.sql("SELECT title FROM books WHERE id IN (41, 42) ORDER BY id")
                .query(String.class).list();

        assertThat(book1).containsExactlyInAnyOrderEntriesOf(
                Map.of("title", "Effective Java", "author", "Joshua Bloch", "price", 36000L, "quantity", 10));
        assertThat(outOfStock).containsExactly(59L, 60L);
        assertThat(koreanTitles).containsExactly("진달래꽃", "운수 좋은 날");
    }

    @Test
    @DisplayName("member 1 is Minji, member 20 the only admin, and every password hash is BCrypt")
    void members() {
        var minji = jdbc.sql("SELECT name, email, role FROM members WHERE id = 1").query().singleRow();
        List<Long> admins = jdbc.sql("SELECT id FROM members WHERE role = 'ADMIN'").query(Long.class).list();
        long bcryptHashes = jdbc.sql("SELECT COUNT(*) FROM members WHERE password_hash LIKE '{bcrypt}$2a$10$%'")
                .query(Long.class).single();

        assertThat(minji).containsExactlyInAnyOrderEntriesOf(
                Map.of("name", "Minji Kim", "email", "minji@example.com", "role", "USER"));
        assertThat(admins).containsExactly(20L);
        assertThat(bcryptHashes).isEqualTo(20);
    }

    @Test
    @DisplayName("Minji has 100,000 won, the admin has 0, and every wallet's history adds up to its balance")
    void wallets() {
        long minji = jdbc.sql("SELECT balance FROM wallets WHERE member_id = 1").query(Long.class).single();
        long admin = jdbc.sql("SELECT balance FROM wallets WHERE member_id = 20").query(Long.class).single();
        List<Long> walletsThatDontAddUp = jdbc.sql("""
                SELECT w.id FROM wallets w LEFT JOIN wallet_transactions t ON t.wallet_id = w.id
                GROUP BY w.id, w.balance
                HAVING w.balance <> COALESCE(SUM(CASE WHEN t.type IN ('CHARGE', 'REFUND', 'TRANSFER_IN')
                                                     THEN t.amount ELSE -t.amount END), 0)""")
                .query(Long.class).list();

        assertThat(minji).isEqualTo(100_000);
        assertThat(admin).isZero();
        assertThat(walletsThatDontAddUp).isEmpty();
    }

    @Test
    @DisplayName("the coupons are issued to and used by the documented members")
    void coupons() {
        List<Map<String, Object>> issues = jdbc.sql("""
                SELECT c.code, MIN(i.member_id) AS first_member, MAX(i.member_id) AS last_member,
                       COUNT(i.id) AS issues, MAX(c.issued_quantity) AS issued_quantity
                FROM coupons c LEFT JOIN coupon_issues i ON i.coupon_id = c.id
                GROUP BY c.code ORDER BY c.code""").query().listOfRows();
        List<String> used = jdbc.sql("""
                SELECT CONCAT(c.code, ' by member ', i.member_id)
                FROM coupon_issues i JOIN coupons c ON c.id = i.coupon_id
                WHERE i.used_at IS NOT NULL ORDER BY c.code, i.member_id""").query(String.class).list();

        assertThat(issues).extracting(row -> row.get("code"), row -> row.get("first_member"),
                        row -> row.get("last_member"), row -> row.get("issues"), row -> row.get("issued_quantity"))
                .containsExactly(
                        tuple("AUTUMN3000", 1L, 10L, 10L, 10),
                        tuple("EXPIRED2000", 2L, 6L, 5L, 5),
                        tuple("FLASH10000", null, null, 0L, 0),
                        tuple("WELCOME5000", 1L, 15L, 15L, 15));
        assertThat(used).containsExactly(
                "AUTUMN3000 by member 1", "WELCOME5000 by member 2", "WELCOME5000 by member 3");
    }

    @Test
    @DisplayName("the 200 orders have the documented statuses, and Minji placed 14 of them")
    void orders() {
        Map<String, Long> statuses = jdbc.sql("SELECT status, COUNT(*) AS orders FROM orders GROUP BY status")
                .query((row, rowNumber) -> Map.entry(row.getString("status"), row.getLong("orders")))
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        long minji = jdbc.sql("SELECT COUNT(*) FROM orders WHERE member_id = 1").query(Long.class).single();

        assertThat(statuses).containsExactlyInAnyOrderEntriesOf(
                Map.of("PAID", 108L, "SHIPPED", 48L, "PENDING", 33L, "CANCELLED", 11L));
        assertThat(minji).isEqualTo(14);
    }

    @Test
    @DisplayName("every paid or shipped order has exactly one approved payment, and no other order has one")
    void payments() {
        long paidOrdersWithOnePayment = jdbc.sql("""
                SELECT COUNT(*) FROM orders o
                WHERE o.status IN ('PAID', 'SHIPPED')
                  AND (SELECT COUNT(*) FROM payments p WHERE p.order_id = o.id AND p.status = 'APPROVED') = 1""")
                .query(Long.class).single();
        long otherPayments = jdbc.sql("""
                SELECT COUNT(*) FROM payments p JOIN orders o ON o.id = p.order_id
                WHERE o.status NOT IN ('PAID', 'SHIPPED')""").query(Long.class).single();

        assertThat(paidOrdersWithOnePayment).isEqualTo(156);
        assertThat(otherPayments).isZero();
    }

    @Test
    @DisplayName("book 1 has 15 reviews, and no member reviewed the same book twice")
    void reviews() {
        long book1 = jdbc.sql("SELECT COUNT(*) FROM reviews WHERE book_id = 1").query(Long.class).single();
        long duplicates = jdbc.sql("""
                SELECT COUNT(*) FROM (SELECT book_id, member_id FROM reviews GROUP BY book_id, member_id
                                      HAVING COUNT(*) > 1) AS duplicate""").query(Long.class).single();

        assertThat(book1).isEqualTo(15);
        assertThat(duplicates).isZero();
    }
}
