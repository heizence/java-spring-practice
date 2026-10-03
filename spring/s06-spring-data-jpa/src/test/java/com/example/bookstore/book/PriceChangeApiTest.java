package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import com.example.bookstore.book.PriceChangeResponse.BookPriceChange;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.SqlStatementCounter;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code POST /api/categories/{categoryId}/price-changes}. Prices before and after the change are read with plain SQL.
 * Category 1 is Programming and category 4 is Fiction; every price in the seed data is a multiple of 1,000 won.
 */
@IntegrationTest
@DisplayName("S06-07 · Bulk updates")
class PriceChangeApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Autowired
    private BookRepository books;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private TestClock clock;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("raises every price in the category by the percentage")
        void raisesThePrices() {
            List<BookRow> before = booksOf(1);

            assertThat(changePrices(1, 10)).hasStatusOk();

            assertThat(booksOf(1)).extracting(BookRow::price)
                    .containsExactlyElementsOf(before.stream().map(book -> book.price() * 110 / 100).toList());
        }

        @Test
        @DisplayName("answers with the number of changed books and each book's old and new price")
        void describesTheChange() {
            List<BookRow> before = booksOf(1);
            var expected = new PriceChangeResponse(1L, "Programming", 10, before.size(), before.stream()
                    .map(book -> new BookPriceChange(book.id(), book.title(), book.price(), book.price() * 110 / 100))
                    .toList());

            assertThat(changePrices(1, 10))
                    .hasStatusOk()
                    .bodyJson().convertTo(PriceChangeResponse.class)
                    .isEqualTo(expected);
        }

        @Test
        @DisplayName("lowers the prices with a negative percentage: -15 % in Fiction")
        void lowersThePrices() {
            List<BookRow> before = booksOf(4);

            assertThat(changePrices(4, -15)).hasStatusOk();

            assertThat(booksOf(4)).extracting(BookRow::price)
                    .containsExactlyElementsOf(before.stream().map(book -> book.price() * 85 / 100).toList());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("leaves the books of other categories alone, prices and updated_at")
        void leavesOtherCategoriesAlone() {
            List<String> before = otherCategoriesThan(1);

            assertThat(changePrices(1, 10)).hasStatusOk();

            assertThat(otherCategoriesThan(1)).isEqualTo(before);
        }

        @Test
        @DisplayName("sets updated_at of every changed book to the current time, like any other change")
        void updatesTheTimestamp() {
            Instant now = Instant.parse("2026-10-20T09:30:00Z");
            clock.setInstant(now);

            assertThat(changePrices(1, 10)).hasStatusOk();

            assertThat(jdbc.sql("SELECT updated_at FROM books WHERE category_id = 1")
                    .query((row, rowNumber) -> row.getTimestamp("updated_at").toInstant()).list())
                    .isNotEmpty()
                    .containsOnly(now);
        }

        @Test
        @DisplayName("a book loaded earlier in the same transaction shows its new price afterwards")
        void noStaleBooks() {
            Book loadedBefore = books.findById(1L).orElseThrow();
            assertThat(loadedBefore.getPrice()).isEqualTo(36_000);

            assertThat(changePrices(1, 10)).hasStatusOk();

            assertThat(books.findById(1L).orElseThrow().getPrice()).isEqualTo(39_600);
        }

        @Test
        @DisplayName("a category without books changes nothing")
        void emptyCategory() {
            Category poetry = categories.save(Category.create("Poetry"));

            assertThat(changePrices(poetry.getId(), 10))
                    .hasStatusOk()
                    .bodyJson().convertTo(PriceChangeResponse.class)
                    .isEqualTo(new PriceChangeResponse(poetry.getId(), "Poetry", 10, 0, List.of()));
        }
    }

    @Nested
    @DisplayName("Queries")
    class Queries {

        @Test
        @DisplayName("changes all prices with one UPDATE statement, however many books the category has")
        void oneUpdateStatement() {
            assertThat(changePrices(1, 10)).hasStatusOk();

            assertThat(sqlStatements.statements()).as("%s", sqlStatements)
                    .filteredOn(sql -> sql.startsWith("update"))
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("answers 404 CATEGORY_NOT_FOUND for a category that doesn't exist")
        void unknownCategory() {
            assertThat(changePrices(999, 10))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Category 999 was not found.",
                              "instance": "/api/categories/999/price-changes",
                              "code": "CATEGORY_NOT_FOUND"
                            }""");
        }

        @Test
        @DisplayName("answers 400 INVALID_INPUT for a percentage outside -50 to 50, and changes nothing")
        void percentOutOfRange() {
            List<BookRow> before = booksOf(1);

            assertThat(changePrices(1, 51))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("INVALID_INPUT"));

            assertThat(booksOf(1)).isEqualTo(before);
        }
    }

    /**
     * Without the test transaction, the request's own transaction commits, as in production. That also shows whether
     * the service starts a transaction at all: an UPDATE query needs one.
     */
    @Nested
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @DisplayName("Without the test transaction")
    class WithoutTheTestTransaction {

        @Autowired
        private SeedData seedData;

        @AfterEach
        void restoreSeedData() {
            seedData.restore();
        }

        @Test
        @DisplayName("commits the new prices when the request ends")
        void commits() {
            assertThat(changePrices(1, 10)).hasStatusOk();

            assertThat(jdbc.sql("SELECT price FROM books WHERE id = 1").query(Long.class).single()).isEqualTo(39_600);
        }
    }

    private record BookRow(Long id, String title, long price) {
    }

    private MvcTestResult changePrices(long categoryId, int percent) {
        return mvc.post().uri("/api/categories/{id}/price-changes", categoryId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "percent": %d }""".formatted(percent))
                .exchange();
    }

    /** The books of a category in id order, read with plain SQL. */
    private List<BookRow> booksOf(long categoryId) {
        return jdbc.sql("SELECT id, title, price FROM books WHERE category_id = ? ORDER BY id")
                .param(categoryId)
                .query((row, rowNumber) -> new BookRow(row.getLong("id"), row.getString("title"), row.getLong("price")))
                .list();
    }

    /** "id price updated_at" for every book outside the category. */
    private List<String> otherCategoriesThan(long categoryId) {
        return jdbc.sql("SELECT id, price, updated_at FROM books WHERE category_id <> ? ORDER BY id")
                .param(categoryId)
                .query((row, rowNumber) -> row.getLong("id") + " " + row.getLong("price") + " "
                        + row.getTimestamp("updated_at").toInstant())
                .list();
    }
}
