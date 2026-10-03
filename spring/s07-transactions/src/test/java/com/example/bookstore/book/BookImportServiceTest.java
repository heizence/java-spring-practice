package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.tuple;

import java.time.LocalDate;
import java.util.List;

import com.example.bookstore.book.BookImportReport.FailedChunk;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SeedData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link BookImportService#importBooks}: one transaction per chunk.
 *
 * <p>No test transaction ({@code NOT_SUPPORTED}): each chunk must really commit or roll back on its own. The rows use
 * ISBNs that start with {@code 9790000}, which no seeded book has. Each test puts the seed data back at the end.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S07-06 · Programmatic transactions")
class BookImportServiceTest {

    private static final String UNKNOWN_CATEGORY = "Poetry";

    @Autowired
    private BookImportService bookImportService;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SeedData seedData;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("imports every row, chunk by chunk, and reports them all")
        void importsEveryRow() {
            List<BookImportRow> rows = List.of(row(1), row(2), row(3), row(4), row(5));

            BookImportReport report = bookImportService.importBooks(rows, 2);

            assertThat(report).isEqualTo(new BookImportReport(5, 5, List.of()));
            assertThat(importedIsbns()).containsExactly(isbn(1), isbn(2), isbn(3), isbn(4), isbn(5));
        }

        @Test
        @DisplayName("saves each book with its category, price and stock")
        void savesTheBookAndItsStock() {
            bookImportService.importBooks(List.of(row(1)), 10);

            var saved = jdbc.sql("""
                    SELECT b.title, c.name AS category, b.price, i.quantity
                    FROM books b JOIN categories c ON c.id = b.category_id JOIN inventories i ON i.book_id = b.id
                    WHERE b.isbn = ?""").param(isbn(1)).query().singleRow();
            assertThat(saved).containsEntry("title", "Imported Book 1").containsEntry("category", "Programming")
                    .containsEntry("price", 20_000L).containsEntry("quantity", 5);
        }

        @Test
        @DisplayName("a bad row rolls back its own chunk only: its good rows aren't saved, the other chunks are")
        void badRowRollsBackItsChunk() {
            List<BookImportRow> rows = List.of(row(1), row(2), row(3), withCategory(row(4), UNKNOWN_CATEGORY), row(5));

            BookImportReport report = bookImportService.importBooks(rows, 2);

            assertThat(report).isEqualTo(new BookImportReport(5, 3,
                    List.of(new FailedChunk(3, 4, "Category 'Poetry' was not found."))));
            assertThat(importedIsbns()).containsExactly(isbn(1), isbn(2), isbn(5));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a failed first chunk doesn't stop the import: the later chunks are saved")
        void failedFirstChunk() {
            List<BookImportRow> rows = List.of(withPrice(row(1), 0), row(2), row(3), row(4));

            BookImportReport report = bookImportService.importBooks(rows, 2);

            assertThat(report.importedRows()).isEqualTo(2);
            assertThat(report.failedChunks()).extracting(FailedChunk::firstRow, FailedChunk::lastRow)
                    .containsExactly(tuple(1, 2));
            assertThat(importedIsbns()).containsExactly(isbn(3), isbn(4));
        }

        @Test
        @DisplayName("the last chunk may be smaller than the others, and is imported too")
        void smallerLastChunk() {
            List<BookImportRow> rows = List.of(row(1), row(2), row(3), row(4), row(5));

            BookImportReport report = bookImportService.importBooks(rows, 3);

            assertThat(report).isEqualTo(new BookImportReport(5, 5, List.of()));
            assertThat(importedIsbns()).hasSize(5);
        }

        @Test
        @DisplayName("a bad row in a smaller last chunk is reported with that chunk's row numbers")
        void badRowInTheLastChunk() {
            List<BookImportRow> rows = List.of(row(1), row(2), row(3), row(4), withCategory(row(5), UNKNOWN_CATEGORY));

            BookImportReport report = bookImportService.importBooks(rows, 2);

            assertThat(report).isEqualTo(new BookImportReport(5, 4,
                    List.of(new FailedChunk(5, 5, "Category 'Poetry' was not found."))));
            assertThat(importedIsbns()).containsExactly(isbn(1), isbn(2), isbn(3), isbn(4));
        }

        @Test
        @DisplayName("a chunk size larger than the list makes one chunk, and Integer.MAX_VALUE doesn't overflow")
        void oneBigChunk() {
            List<BookImportRow> rows = List.of(row(1), row(2), row(3));

            BookImportReport report = bookImportService.importBooks(rows, Integer.MAX_VALUE);

            assertThat(report).isEqualTo(new BookImportReport(3, 3, List.of()));
            assertThat(importedIsbns()).hasSize(3);
        }

        @Test
        @DisplayName("an ISBN that is already in the catalog fails its chunk")
        void isbnAlreadyInTheCatalog() {
            String effectiveJavaIsbn = jdbc.sql("SELECT isbn FROM books WHERE id = 1").query(String.class).single();
            List<BookImportRow> rows = List.of(row(1), withIsbn(row(2), effectiveJavaIsbn), row(3));

            BookImportReport report = bookImportService.importBooks(rows, 2);

            assertThat(report.failedChunks()).containsExactly(new FailedChunk(1, 2,
                    "A book with ISBN %s already exists.".formatted(effectiveJavaIsbn)));
            assertThat(importedIsbns()).containsExactly(isbn(3));
        }

        @Test
        @DisplayName("the same ISBN twice in one chunk fails that chunk")
        void sameIsbnTwiceInAChunk() {
            List<BookImportRow> rows = List.of(row(1), withIsbn(row(2), isbn(1)), row(3));

            BookImportReport report = bookImportService.importBooks(rows, 2);

            assertThat(report.importedRows()).isEqualTo(1);
            assertThat(report.failedChunks()).extracting(FailedChunk::firstRow).containsExactly(1);
            assertThat(importedIsbns()).containsExactly(isbn(3));
        }

        @Test
        @DisplayName("when every chunk fails, nothing is imported and the report lists every chunk")
        void everyChunkFails() {
            List<BookImportRow> rows = List.of(withPrice(row(1), 0), withCategory(row(2), UNKNOWN_CATEGORY),
                    withPrice(row(3), -5));

            BookImportReport report = bookImportService.importBooks(rows, 1);

            assertThat(report.importedRows()).isZero();
            assertThat(report.failedChunks()).extracting(FailedChunk::firstRow).containsExactly(1, 2, 3);
            assertThat(importedIsbns()).isEmpty();
        }

        @Test
        @DisplayName("an empty list imports nothing and reports nothing")
        void emptyList() {
            BookImportReport report = bookImportService.importBooks(List.of(), 2);

            assertThat(report).isEqualTo(new BookImportReport(0, 0, List.of()));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "chunk size {0}")
        @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
        @DisplayName("rejects a chunk size below 1")
        void rejectsChunkSizeBelowOne(int chunkSize) {
            assertThatIllegalArgumentException().isThrownBy(() -> bookImportService.importBooks(List.of(row(1)),
                    chunkSize));
            assertThat(importedIsbns()).isEmpty();
        }

        @Test
        @DisplayName("rejects null rows")
        void rejectsNullRows() {
            assertThatIllegalArgumentException().isThrownBy(() -> bookImportService.importBooks(null, 2));
        }
    }

    /** A valid row: a Programming book with ISBN 979000000000n, 20,000 won, 5 copies. */
    private static BookImportRow row(int n) {
        return new BookImportRow(isbn(n), "Imported Book " + n, "An Author", "Programming", 20_000,
                LocalDate.of(2026, 9, 1), 5);
    }

    private static String isbn(int n) {
        return "979000000%04d".formatted(n);
    }

    private static BookImportRow withCategory(BookImportRow row, String category) {
        return new BookImportRow(row.isbn(), row.title(), row.author(), category, row.price(), row.publishedDate(),
                row.stock());
    }

    private static BookImportRow withPrice(BookImportRow row, long price) {
        return new BookImportRow(row.isbn(), row.title(), row.author(), row.category(), price, row.publishedDate(),
                row.stock());
    }

    private static BookImportRow withIsbn(BookImportRow row, String isbn) {
        return new BookImportRow(isbn, row.title(), row.author(), row.category(), row.price(), row.publishedDate(),
                row.stock());
    }

    /** The ISBNs of the books saved by the test, in ISBN order. */
    private List<String> importedIsbns() {
        return jdbc.sql("SELECT isbn FROM books WHERE isbn LIKE '9790000%' ORDER BY isbn").query(String.class).list();
    }
}
