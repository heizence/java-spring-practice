package com.example.bookstore.wishlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import com.example.bookstore.book.Book;
import com.example.bookstore.member.Member;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SqlStatementCounter;
import com.example.bookstore.support.TestClock;
import jakarta.persistence.EntityManager;
import org.hibernate.Hibernate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;

/**
 * The mapping of {@link WishlistItem} to the table {@code wishlist_items}, checked in both directions: what the entity
 * writes is read back with plain SQL, and rows written with plain SQL are read through the entity. The tests use the
 * {@link EntityManager} directly, because there is no repository for wishlist items yet.
 */
@IntegrationTest
@DisplayName("S06-01 · Mapping entities")
class WishlistItemMappingTest {

    private static final Instant NOW = TestClock.DEFAULT_INSTANT;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TestClock clock;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("saves a new item, and MySQL generates its id")
        void generatesTheId() {
            WishlistItem item = WishlistItem.add(minji(), effectiveJava(), WishlistPriority.HIGH,
                    "For the team library");

            entityManager.persist(item);
            entityManager.flush();

            assertThat(item.getId()).isNotNull();
            assertThat(jdbc.sql("SELECT COUNT(*) FROM wishlist_items WHERE id = ?").param(item.getId())
                    .query(Long.class).single()).isOne();
        }

        @Test
        @DisplayName("writes each field to its column: member_id, book_id, priority (by name) and memo")
        void writesEachColumn() {
            WishlistItem item = save(WishlistItem.add(minji(), effectiveJava(), WishlistPriority.HIGH,
                    "For the team library"));

            assertThat(row(item.getId()))
                    .containsEntry("member_id", 1L)
                    .containsEntry("book_id", 1L)
                    .containsEntry("priority", "HIGH")
                    .containsEntry("memo", "For the team library");
        }

        @Test
        @DisplayName("fills created_at and updated_at from the clock")
        void fillsTheTimestamps() {
            WishlistItem item = save(WishlistItem.add(minji(), effectiveJava(), WishlistPriority.MEDIUM, null));

            assertThat(timestamps(item.getId())).containsExactly(NOW, NOW);
        }

        @Test
        @DisplayName("reads a row that was written with plain SQL")
        void readsARow() {
            long id = insertRow(1L, 1L, "LOW", "Only if it's on sale");

            WishlistItem item = entityManager.find(WishlistItem.class, id);

            assertThat(item.getId()).isEqualTo(id);
            assertThat(item.getMember().getName()).isEqualTo("Minji Kim");
            assertThat(item.getBook().getTitle()).isEqualTo("Effective Java");
            assertThat(item.getPriority()).isEqualTo(WishlistPriority.LOW);
            assertThat(item.getNote()).contains("Only if it's on sale");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("stores an item without a note as NULL, and reads it back with an empty note")
        void noNote() {
            WishlistItem item = save(WishlistItem.add(minji(), effectiveJava(), WishlistPriority.MEDIUM, null));
            entityManager.clear();

            WishlistItem reloaded = entityManager.find(WishlistItem.class, item.getId());

            assertThat(row(item.getId())).containsEntry("memo", null);
            assertThat(reloaded.getNote()).isEmpty();
        }

        @Test
        @DisplayName("stores a note without its surrounding whitespace, and a blank note as NULL")
        void normalizesTheNote() {
            WishlistItem padded = save(WishlistItem.add(minji(), effectiveJava(), WishlistPriority.HIGH,
                    "  A gift for Jun \n"));
            WishlistItem blank = save(WishlistItem.add(member(2L), effectiveJava(), WishlistPriority.HIGH, " \t "));

            assertThat(row(padded.getId())).containsEntry("memo", "A gift for Jun");
            assertThat(row(blank.getId())).containsEntry("memo", null);
            assertThat(blank.getNote()).isEmpty();
        }

        @Test
        @DisplayName("stores a note of exactly 200 characters")
        void longestNote() {
            String note = "x".repeat(WishlistItem.MAX_NOTE_LENGTH);

            WishlistItem item = save(WishlistItem.add(minji(), effectiveJava(), WishlistPriority.LOW, note));

            assertThat(row(item.getId())).containsEntry("memo", note);
        }

        @Test
        @DisplayName("loads the member and the book only when they are used (LAZY)")
        void loadsTheAssociationsLazily() {
            long id = insertRow(1L, 1L, "HIGH", null);

            WishlistItem item = entityManager.find(WishlistItem.class, id);

            assertThat(Hibernate.isInitialized(item.getMember())).as("member loaded with the item").isFalse();
            assertThat(Hibernate.isInitialized(item.getBook())).as("book loaded with the item").isFalse();
            assertThat(sqlStatements.count()).as("%s", sqlStatements).isOne();
        }

        @Test
        @DisplayName("saves a changed priority and note without a save() call, and moves only updated_at")
        void savesChanges() {
            WishlistItem item = save(WishlistItem.add(minji(), effectiveJava(), WishlistPriority.LOW, "Maybe later"));
            clock.advance(Duration.ofDays(3));

            item.changePriority(WishlistPriority.HIGH);
            item.changeNote("Needed for the new project");
            entityManager.flush();

            assertThat(row(item.getId()))
                    .containsEntry("priority", "HIGH")
                    .containsEntry("memo", "Needed for the new project");
            assertThat(timestamps(item.getId())).containsExactly(NOW, NOW.plus(Duration.ofDays(3)));
        }

        @Test
        @DisplayName("removing an item deletes only its own row, not its member or its book")
        void removesOnlyTheItem() {
            WishlistItem item = save(WishlistItem.add(minji(), effectiveJava(), WishlistPriority.HIGH, null));

            entityManager.remove(item);
            entityManager.flush();

            assertThat(jdbc.sql("SELECT COUNT(*) FROM wishlist_items WHERE id = ?").param(item.getId())
                    .query(Long.class).single()).isZero();
            assertThat(jdbc.sql("SELECT COUNT(*) FROM books WHERE id = 1").query(Long.class).single()).isOne();
            assertThat(jdbc.sql("SELECT COUNT(*) FROM members WHERE id = 1").query(Long.class).single()).isOne();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a missing member, book or priority with IllegalArgumentException")
        void rejectsMissingParts() {
            assertThatThrownBy(() -> WishlistItem.add(null, effectiveJava(), WishlistPriority.HIGH, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> WishlistItem.add(minji(), null, WishlistPriority.HIGH, null))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> WishlistItem.add(minji(), effectiveJava(), null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects a note longer than 200 characters")
        void rejectsALongNote() {
            String note = "x".repeat(WishlistItem.MAX_NOTE_LENGTH + 1);

            assertThatThrownBy(() -> WishlistItem.add(minji(), effectiveJava(), WishlistPriority.HIGH, note))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects an invalid change and keeps the item as it was")
        void rejectsInvalidChanges() {
            WishlistItem item = WishlistItem.add(minji(), effectiveJava(), WishlistPriority.LOW, "Maybe later");

            assertThatThrownBy(() -> item.changePriority(null)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> item.changeNote("x".repeat(WishlistItem.MAX_NOTE_LENGTH + 1)))
                    .isInstanceOf(IllegalArgumentException.class);

            assertThat(item.getPriority()).isEqualTo(WishlistPriority.LOW);
            assertThat(item.getNote()).contains("Maybe later");
        }
    }

    private Member minji() {
        return member(1L);
    }

    private Member member(long id) {
        return entityManager.getReference(Member.class, id);
    }

    private Book effectiveJava() {
        return entityManager.getReference(Book.class, 1L);
    }

    private WishlistItem save(WishlistItem item) {
        entityManager.persist(item);
        entityManager.flush();
        return item;
    }

    /** The columns the entity maps, read with plain SQL. */
    private Map<String, Object> row(long id) {
        return jdbc.sql("SELECT member_id, book_id, priority, memo FROM wishlist_items WHERE id = ?")
                .param(id).query().singleRow();
    }

    /** created_at and updated_at, in that order. */
    private List<Instant> timestamps(long id) {
        return jdbc.sql("SELECT created_at, updated_at FROM wishlist_items WHERE id = ?").param(id)
                .query((row, rowNumber) -> List.of(row.getTimestamp("created_at").toInstant(),
                        row.getTimestamp("updated_at").toInstant()))
                .single();
    }

    /** Inserts a row with plain SQL, as another program would, and returns its id. */
    private long insertRow(long memberId, long bookId, String priority, String memo) {
        Timestamp created = Timestamp.from(Instant.parse("2026-09-01T10:00:00Z"));
        var keyHolder = new GeneratedKeyHolder();
        jdbc.sql("""
                        INSERT INTO wishlist_items (member_id, book_id, priority, memo, created_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?)""")
                .params(memberId, bookId, priority, memo, created, created)
                .update(keyHolder);
        return keyHolder.getKey().longValue(); // MySQL returns generated keys as BigInteger
    }
}
