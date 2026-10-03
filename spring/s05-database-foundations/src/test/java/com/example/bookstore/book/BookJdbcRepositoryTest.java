package com.example.bookstore.book;

import static com.example.bookstore.book.BookTestData.insertBook;
import static com.example.bookstore.book.BookTestData.insertCategory;
import static com.example.bookstore.book.BookTestData.newIsbn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Runs {@link BookJdbcRepository} against MySQL with the seed data. Each test runs in a transaction that is rolled
 * back, so the books it inserts disappear afterwards. No real book title contains "Quokka", so a search for it finds
 * only the books a test inserted.
 */
@IntegrationTest
@DisplayName("S05-02 · Queries with JdbcClient")
class BookJdbcRepositoryTest {

    private static final long PROGRAMMING = 1L;
    private static final long FICTION = 4L;
    private static final LocalDate PUBLISHED = LocalDate.of(2024, 5, 1);
    private static final Book EFFECTIVE_JAVA = new Book(1L, "9791115881394", "Effective Java", "Joshua Bloch",
            "Programming", 36_000, LocalDate.of(2018, 10, 18));

    @Autowired
    private BookJdbcRepository books;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TestClock clock;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("findById() returns the book with its category's name")
        void findsABook() {
            assertThat(books.findById(1L)).contains(EFFECTIVE_JAVA);
        }

        @Test
        @DisplayName("searchByTitle() returns the books whose title contains the keyword, sorted by title")
        void searchesTitles() {
            insertBook(jdbc, PROGRAMMING, "The Quokka Way", "Ann Lee", 30_000, PUBLISHED);
            insertBook(jdbc, PROGRAMMING, "Quokka Patterns", "Ben Kim", 20_000, PUBLISHED);
            insertBook(jdbc, FICTION, "Island Life", "Cara Quokka", 25_000, PUBLISHED); // only the author matches

            assertThat(books.searchByTitle("Quokka")).extracting(Book::title)
                    .containsExactly("Quokka Patterns", "The Quokka Way");
        }

        @Test
        @DisplayName("findByCategory() returns the first page of a category's books, sorted by id")
        void returnsTheFirstPage() {
            long poetry = insertCategory(jdbc, "Poetry");
            List<Long> ids = insertPoems(poetry, 5);

            assertThat(books.findByCategory(poetry, 0, 2)).extracting(Book::id)
                    .containsExactly(ids.get(0), ids.get(1));
        }

        @Test
        @DisplayName("insert() returns the generated id, and findById() reads the new book back")
        void insertsABook() {
            String isbn = newIsbn();

            long id = books.insert(new NewBook(isbn, "Quokka Patterns", "Ben Kim", PROGRAMMING, 20_000, PUBLISHED));

            assertThat(books.findById(id))
                    .contains(new Book(id, isbn, "Quokka Patterns", "Ben Kim", "Programming", 20_000, PUBLISHED));
        }

        @Test
        @DisplayName("updatePrice() stores the new price and returns 1")
        void updatesThePrice() {
            int changed = books.updatePrice(1L, 38_000);

            assertThat(changed).isEqualTo(1);
            assertThat(priceOf(1L)).isEqualTo(38_000);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("findById() returns empty for an id no book has")
        void unknownId() {
            assertThat(books.findById(99_999L)).isEmpty();
        }

        @Test
        @DisplayName("searchByTitle() ignores letter case")
        void ignoresCase() {
            assertThat(books.searchByTitle("EFFECTIVE java")).contains(EFFECTIVE_JAVA);
        }

        @Test
        @DisplayName("searchByTitle() finds Korean titles")
        void findsKoreanTitles() {
            assertThat(books.searchByTitle("진달래")).extracting(Book::id).contains(41L);
        }

        @Test
        @DisplayName("searchByTitle() treats a quote in the keyword as an ordinary character")
        void keywordWithAQuote() {
            long id = insertBook(jdbc, FICTION, "The Quokka's Guide", "Dan Park", 15_000, PUBLISHED);

            assertThat(books.searchByTitle("Quokka's")).extracting(Book::id).containsExactly(id);
        }

        @Test
        @DisplayName("searchByTitle() sorts books with the same title by id")
        void sameTitleSortedById() {
            long first = insertBook(jdbc, PROGRAMMING, "Quokka Twins", "Eve Cho", 10_000, PUBLISHED);
            long second = insertBook(jdbc, FICTION, "Quokka Twins", "Fay Oh", 12_000, PUBLISHED);

            assertThat(books.searchByTitle("Quokka Twins")).extracting(Book::id).containsExactly(first, second);
        }

        @Test
        @DisplayName("searchByTitle() returns an empty list when no title matches")
        void noMatch() {
            assertThat(books.searchByTitle("no title has these words")).isEmpty();
        }

        @Test
        @DisplayName("findByCategory() sorts by id, not by title")
        void sortsById() {
            long poetry = insertCategory(jdbc, "Poetry");
            List<Long> ids = insertPoems(poetry, 3); // titles in reverse alphabetical order

            assertThat(books.findByCategory(poetry, 0, 10)).extracting(Book::id).containsExactlyElementsOf(ids);
        }

        @Test
        @DisplayName("findByCategory() returns a partial last page")
        void partialLastPage() {
            long poetry = insertCategory(jdbc, "Poetry");
            List<Long> ids = insertPoems(poetry, 5);

            assertThat(books.findByCategory(poetry, 1, 2)).extracting(Book::id)
                    .containsExactly(ids.get(2), ids.get(3));
            assertThat(books.findByCategory(poetry, 2, 2)).extracting(Book::id).containsExactly(ids.get(4));
        }

        @Test
        @DisplayName("findByCategory() returns an empty list for a page after the last one")
        void pageAfterTheLast() {
            long poetry = insertCategory(jdbc, "Poetry");
            insertPoems(poetry, 5);

            assertThat(books.findByCategory(poetry, 3, 2)).isEmpty();
        }

        @Test
        @DisplayName("findByCategory() returns an empty list for a page so far out that page × size overflows an int")
        void pageFarOut() {
            assertThat(books.findByCategory(PROGRAMMING, Integer.MAX_VALUE, BookJdbcRepository.MAX_PAGE_SIZE))
                    .isEmpty();
        }

        @Test
        @DisplayName("findByCategory() accepts the smallest and the largest page size, 1 and 100")
        void pageSizeLimits() {
            long poetry = insertCategory(jdbc, "Poetry");
            List<Long> ids = insertPoems(poetry, 3);

            assertThat(books.findByCategory(poetry, 1, 1)).extracting(Book::id).containsExactly(ids.get(1));
            assertThat(books.findByCategory(poetry, 0, 100)).extracting(Book::id).containsExactlyElementsOf(ids);
        }

        @Test
        @DisplayName("findByCategory() returns an empty list for a category without books")
        void emptyCategory() {
            long poetry = insertCategory(jdbc, "Poetry");

            assertThat(books.findByCategory(poetry, 0, 10)).isEmpty();
        }

        @Test
        @DisplayName("insert() sets created_at and updated_at to the clock's time, with microseconds")
        void setsTheTimes() {
            Instant now = Instant.parse("2026-10-15T09:30:00.123456Z");
            clock.setInstant(now);

            long id = books.insert(aNewBook());

            assertThat(createdAndUpdatedAt(id)).containsExactly(now, now);
        }

        @Test
        @DisplayName("insert() returns a new, larger id for each book")
        void newIdForEachBook() {
            long first = books.insert(aNewBook());
            long second = books.insert(aNewBook());

            assertThat(first).isGreaterThan(60L); // the seed data has books 1 to 60
            assertThat(second).isGreaterThan(first);
        }

        @Test
        @DisplayName("insert() keeps a Korean title and author")
        void keepsKoreanText() {
            long id = books.insert(new NewBook(newIsbn(), "님의 침묵", "한용운", FICTION, 12_000,
                    LocalDate.of(1926, 5, 20)));

            assertThat(books.findById(id)).get()
                    .returns("님의 침묵", Book::title)
                    .returns("한용운", Book::author)
                    .returns("Fiction", Book::category);
        }

        @Test
        @DisplayName("updatePrice() sets updated_at to the clock's time and keeps created_at")
        void setsUpdatedAt() {
            Instant createdAt = createdAndUpdatedAt(1L).getFirst();
            Instant later = Instant.parse("2026-10-16T08:00:00.654321Z");
            clock.setInstant(later);

            books.updatePrice(1L, 38_000);

            assertThat(createdAndUpdatedAt(1L)).containsExactly(createdAt, later);
        }

        @Test
        @DisplayName("updatePrice() changes only the book with that id")
        void changesOnlyThatBook() {
            long changed = insertBook(jdbc, PROGRAMMING, "Quokka Patterns", "Ben Kim", 20_000, PUBLISHED);
            long unchanged = insertBook(jdbc, PROGRAMMING, "The Quokka Way", "Ann Lee", 30_000, PUBLISHED);

            books.updatePrice(changed, 22_000);

            assertThat(priceOf(changed)).isEqualTo(22_000);
            assertThat(priceOf(unchanged)).isEqualTo(30_000);
            assertThat(priceOf(1L)).isEqualTo(36_000);
        }

        @Test
        @DisplayName("updatePrice() returns 0 and changes nothing for an id no book has")
        void updatesNothingForAnUnknownId() {
            Instant later = Instant.parse("2026-10-16T08:00:00.654321Z");
            clock.setInstant(later);

            int changed = books.updatePrice(99_999L, 22_000);

            assertThat(changed).isZero();
            assertThat(jdbc.sql("SELECT COUNT(*) FROM books WHERE updated_at = :later").param("later", later)
                    .query(Long.class).single()).isZero();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "keyword \"{0}\"", quoteTextArguments = false)
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("searchByTitle() rejects a missing or blank keyword")
        void rejectsBlankKeywords(String keyword) {
            assertThatThrownBy(() -> books.searchByTitle(keyword)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("findByCategory() rejects a negative page")
        void rejectsNegativePages() {
            assertThatThrownBy(() -> books.findByCategory(PROGRAMMING, -1, 10))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "size {0}")
        @ValueSource(ints = {0, -1, 101})
        @DisplayName("findByCategory() rejects a page size outside 1 to 100")
        void rejectsInvalidPageSizes(int size) {
            assertThatThrownBy(() -> books.findByCategory(PROGRAMMING, 0, size))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("insert() reports an ISBN that already exists with DuplicateKeyException, and inserts nothing")
        void rejectsADuplicateIsbn() {
            var duplicate = new NewBook("9791115881394", "Effective Java, again", "Someone Else", PROGRAMMING, 10_000,
                    PUBLISHED);

            assertThatThrownBy(() -> books.insert(duplicate)).isInstanceOf(DuplicateKeyException.class);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM books WHERE isbn = '9791115881394'").query(Long.class).single())
                    .isEqualTo(1);
        }

        @Test
        @DisplayName("insert() reports an unknown category with DataIntegrityViolationException")
        void rejectsAnUnknownCategory() {
            var orphan = new NewBook(newIsbn(), "Quokka Patterns", "Ben Kim", 99_999L, 20_000, PUBLISHED);

            assertThatThrownBy(() -> books.insert(orphan))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .isNotInstanceOf(DuplicateKeyException.class);
        }

        @Test
        @DisplayName("insert() rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> books.insert(null)).isInstanceOf(IllegalArgumentException.class);
        }

        @ParameterizedTest(name = "price {0}")
        @ValueSource(longs = {0, -1})
        @DisplayName("updatePrice() rejects a price below 1 and keeps the old one")
        void rejectsNonPositivePrices(long price) {
            assertThatThrownBy(() -> books.updatePrice(1L, price)).isInstanceOf(IllegalArgumentException.class);
            assertThat(priceOf(1L)).isEqualTo(36_000);
        }
    }

    /** Inserts books into a category, with titles in reverse alphabetical order; returns their ids, oldest first. */
    private List<Long> insertPoems(long categoryId, int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = count; i >= 1; i--) {
            ids.add(insertBook(jdbc, categoryId, "Poem " + i, "Poet " + i, 10_000 + i, PUBLISHED));
        }
        return ids;
    }

    private NewBook aNewBook() {
        return new NewBook(newIsbn(), "Quokka Patterns", "Ben Kim", PROGRAMMING, 20_000, PUBLISHED);
    }

    private long priceOf(long bookId) {
        return jdbc.sql("SELECT price FROM books WHERE id = :id").param("id", bookId).query(Long.class).single();
    }

    /** Read with getTimestamp(): JdbcClient can't map a DATETIME column to an Instant directly. */
    private List<Instant> createdAndUpdatedAt(long bookId) {
        return jdbc.sql("SELECT created_at, updated_at FROM books WHERE id = :id")
                .param("id", bookId)
                .query((row, rowNumber) -> List.of(row.getTimestamp("created_at").toInstant(),
                        row.getTimestamp("updated_at").toInstant()))
                .single();
    }
}
