package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Checks the {@code wishlist_items} table twice: through MySQL's own description of the schema
 * ({@code information_schema}), and through behavior, by inserting rows. Each test runs in a transaction that is
 * rolled back, so the rows it inserts disappear afterwards.
 */
@IntegrationTest
@DisplayName("S05-01 · Schema migrations with Flyway")
class WishlistItemsMigrationTest {

    private static final Instant SAVED_AT = Instant.parse("2026-10-15T09:30:00.123456Z");
    private static final long MINJI = 1L;
    private static final long JUN = 2L;
    private static final long EFFECTIVE_JAVA = 1L;
    private static final long CLEAN_CODE = 2L;

    @Autowired
    private JdbcClient jdbc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("Flyway applied a version 3 migration from a file named V3__<description>.sql")
        void appliesVersion3() {
            List<Map<String, Object>> history = jdbc.sql("""
                    SELECT script, success FROM flyway_schema_history WHERE version = '3'""").query().listOfRows();

            assertThat(history).singleElement().satisfies(row -> {
                assertThat((String) row.get("script")).matches("V3__\\w+\\.sql");
                assertThat(row.get("success")).isEqualTo(true);
            });
        }

        @Test
        @DisplayName("the table wishlist_items has exactly the four columns, with their types, all NOT NULL")
        void hasTheColumns() {
            List<Map<String, Object>> columns = jdbc.sql("""
                    SELECT column_name, column_type, is_nullable FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'wishlist_items'""").query().listOfRows();

            assertThat(columns)
                    .extracting(column -> column.get("column_name"), column -> column.get("column_type"),
                            column -> column.get("is_nullable"))
                    .containsExactlyInAnyOrder(
                            tuple("id", "bigint", "NO"),
                            tuple("member_id", "bigint", "NO"),
                            tuple("book_id", "bigint", "NO"),
                            tuple("created_at", "datetime(6)", "NO"));
        }

        @Test
        @DisplayName("a member can save a book, and the row reads back unchanged")
        void savesABook() {
            long id = save(MINJI, EFFECTIVE_JAVA, SAVED_AT);

            Map<String, Object> row = jdbc.sql("SELECT member_id, book_id FROM wishlist_items WHERE id = :id")
                    .param("id", id).query().singleRow();
            assertThat(row).containsEntry("member_id", MINJI).containsEntry("book_id", EFFECTIVE_JAVA);
            assertThat(savedAt(id)).isEqualTo(SAVED_AT);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("id is the primary key, and MySQL generates it")
        void generatesIds() {
            String key = jdbc.sql("""
                    SELECT CONCAT(column_key, ' ', extra) FROM information_schema.columns
                    WHERE table_schema = DATABASE() AND table_name = 'wishlist_items' AND column_name = 'id'""")
                    .query(String.class).single();

            long first = save(MINJI, EFFECTIVE_JAVA, SAVED_AT);
            long second = save(MINJI, CLEAN_CODE, SAVED_AT);

            assertThat(key).isEqualTo("PRI auto_increment");
            assertThat(second).isGreaterThan(first);
        }

        @Test
        @DisplayName("created_at keeps microseconds, like every other DATETIME(6) column")
        void keepsMicroseconds() {
            long id = save(MINJI, EFFECTIVE_JAVA, Instant.parse("2026-10-15T23:59:59.999999Z"));

            assertThat(savedAt(id)).isEqualTo(Instant.parse("2026-10-15T23:59:59.999999Z"));
        }

        @Test
        @DisplayName("one member can save several books, and several members can save the same book")
        void allowsOtherCombinations() {
            save(MINJI, EFFECTIVE_JAVA, SAVED_AT);
            save(MINJI, CLEAN_CODE, SAVED_AT);
            save(JUN, EFFECTIVE_JAVA, SAVED_AT);

            assertThat(jdbc.sql("SELECT COUNT(*) FROM wishlist_items WHERE member_id IN (1, 2)").query(Long.class)
                    .single()).isEqualTo(3);
        }

        @Test
        @DisplayName("the unique key uk_wishlist_items_member_book covers member_id, then book_id")
        void hasTheUniqueKey() {
            assertThat(indexColumns("uk_wishlist_items_member_book", true)).containsExactly("member_id", "book_id");
        }

        @Test
        @DisplayName("the index idx_wishlist_items_book covers book_id")
        void hasTheBookIndex() {
            assertThat(indexColumns("idx_wishlist_items_book", false)).containsExactly("book_id");
        }

        @Test
        @DisplayName("the foreign keys fk_wishlist_items_member and fk_wishlist_items_book point to members and books")
        void hasTheForeignKeys() {
            List<Map<String, Object>> foreignKeys = jdbc.sql("""
                    SELECT constraint_name, column_name, referenced_table_name, referenced_column_name
                    FROM information_schema.key_column_usage
                    WHERE table_schema = DATABASE() AND table_name = 'wishlist_items'
                      AND referenced_table_name IS NOT NULL""").query().listOfRows();

            assertThat(foreignKeys)
                    .extracting(key -> key.get("constraint_name"), key -> key.get("column_name"),
                            key -> key.get("referenced_table_name"), key -> key.get("referenced_column_name"))
                    .containsExactlyInAnyOrder(
                            tuple("fk_wishlist_items_member", "member_id", "members", "id"),
                            tuple("fk_wishlist_items_book", "book_id", "books", "id"));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a book that the member already saved")
        void rejectsTheSameBookTwice() {
            save(MINJI, EFFECTIVE_JAVA, SAVED_AT);

            assertThatThrownBy(() -> save(MINJI, EFFECTIVE_JAVA, SAVED_AT.plusSeconds(60)))
                    .isInstanceOf(DuplicateKeyException.class);
        }

        @Test
        @DisplayName("rejects a member that doesn't exist")
        void rejectsAnUnknownMember() {
            assertThatThrownBy(() -> save(99_999L, EFFECTIVE_JAVA, SAVED_AT))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejects a book that doesn't exist")
        void rejectsAnUnknownBook() {
            assertThatThrownBy(() -> save(MINJI, 99_999L, SAVED_AT))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("rejects a row without created_at")
        void rejectsAMissingTime() {
            assertThatThrownBy(() -> save(MINJI, EFFECTIVE_JAVA, null))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
    }

    /** Inserts a row with plain SQL and returns the id that MySQL generated. */
    private long save(long memberId, long bookId, Instant createdAt) {
        jdbc.sql("INSERT INTO wishlist_items (member_id, book_id, created_at) VALUES (:memberId, :bookId, :createdAt)")
                .param("memberId", memberId)
                .param("bookId", bookId)
                .param("createdAt", createdAt)
                .update();
        return jdbc.sql("SELECT LAST_INSERT_ID()").query(Long.class).single();
    }

    /** Read with getTimestamp(): JdbcClient can't map a DATETIME column to an Instant directly. */
    private Instant savedAt(long id) {
        return jdbc.sql("SELECT created_at FROM wishlist_items WHERE id = :id")
                .param("id", id)
                .query((row, rowNumber) -> row.getTimestamp("created_at").toInstant())
                .single();
    }

    /** The columns of an index, in their order inside it; empty if no index with that name and uniqueness exists. */
    private List<String> indexColumns(String indexName, boolean unique) {
        return jdbc.sql("""
                        SELECT column_name FROM information_schema.statistics
                        WHERE table_schema = DATABASE() AND table_name = 'wishlist_items'
                          AND index_name = :indexName AND non_unique = :nonUnique
                        ORDER BY seq_in_index""")
                .param("indexName", indexName)
                .param("nonUnique", unique ? 0 : 1)
                .query(String.class)
                .list();
    }
}
