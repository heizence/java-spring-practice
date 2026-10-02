package practice.j07.ex04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.charset.MalformedInputException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** Tests for {@link BookCsvImporter} and {@link ImportReport}. Each test writes its own files to a temporary folder. */
@DisplayName("J07-04 · Files and CSV")
class BookCsvImporterTest {

    private static final String HEADER = "isbn,title,author,price";
    private static final ImportError WRONG_HEADER = new ImportError(1, "header must be \"isbn,title,author,price\"");

    private static final Book EFFECTIVE_JAVA =
            new Book("9780134685991", "Effective Java", "Joshua Bloch", new BigDecimal("45.99"));
    private static final Book VEGETARIAN = new Book("9788936434120", "채식주의자", "한강", new BigDecimal("15000"));

    @TempDir
    Path dir;

    /** Writes the lines to a UTF-8 file, each line ending with \n, and returns its path. */
    private Path csv(String... lines) throws IOException {
        return Files.writeString(dir.resolve("books.csv"), String.join("\n", lines) + "\n", StandardCharsets.UTF_8);
    }

    /** Imports a file with the header and one row, and returns the only book in it. */
    private Book importOne(String row) throws IOException {
        ImportResult result = BookCsvImporter.importFrom(csv(HEADER, row));
        assertThat(result.errors()).as("errors for %s", row).isEmpty();
        assertThat(result.books()).as("books for %s", row).hasSize(1);
        return result.books().getFirst();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("imports every row after the header, in file order")
        void importsRows() throws IOException {
            Path file = csv(HEADER,
                    "9780134685991,Effective Java,Joshua Bloch,45.99",
                    "9788936434120,채식주의자,한강,15000");

            ImportResult result = BookCsvImporter.importFrom(file);

            assertThat(result.books()).containsExactly(EFFECTIVE_JAVA, VEGETARIAN);
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("reports a row with an invalid price by its line number, and imports the rows after it")
        void invalidPriceBecomesError() throws IOException {
            Path file = csv(HEADER,
                    "9780134685991,Effective Java,Joshua Bloch,45.99",
                    "9781617294945,Spring in Action,Craig Walls,abc",
                    "9788936434120,채식주의자,한강,15000");

            ImportResult result = BookCsvImporter.importFrom(file);

            assertThat(result.books()).containsExactly(EFFECTIVE_JAVA, VEGETARIAN);
            assertThat(result.errors()).containsExactly(new ImportError(3, "invalid price \"abc\""));
        }

        @Test
        @DisplayName("the report lists the number of books and errors, then every error")
        void writesReport() throws IOException {
            var result = new ImportResult(List.of(EFFECTIVE_JAVA, VEGETARIAN), List.of(
                    new ImportError(3, "invalid price \"abc\""),
                    new ImportError(7, "expected 4 fields but found 3")));
            Path report = dir.resolve("report.txt");

            ImportReport.write(report, result);

            assertThat(Files.readString(report)).isEqualTo("""
                    books: 2
                    errors: 2
                    line 3: invalid price "abc"
                    line 7: expected 4 fields but found 3
                    """);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a file with only the header has no books and no errors")
        void onlyHeader() throws IOException {
            ImportResult result = BookCsvImporter.importFrom(csv(HEADER));

            assertThat(result.books()).isEmpty();
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("skips blank lines, but still counts them in the line numbers")
        void skipsBlankLines() throws IOException {
            Path file = csv(HEADER,
                    "9780134685991,Effective Java,Joshua Bloch,45.99",
                    "",
                    "   ",
                    "9781617294945,Spring in Action,Craig Walls,free");

            ImportResult result = BookCsvImporter.importFrom(file);

            assertThat(result.books()).containsExactly(EFFECTIVE_JAVA);
            assertThat(result.errors()).containsExactly(new ImportError(5, "invalid price \"free\""));
        }

        @Test
        @DisplayName("keeps a comma inside a quoted field")
        void commaInsideQuotes() throws IOException {
            Book book = importOne("9780134685991,\"Effective Java, 3rd Edition\",Joshua Bloch,45.99");

            assertThat(book.title()).isEqualTo("Effective Java, 3rd Edition");
        }

        @Test
        @DisplayName("turns two double quotes inside a quoted field into one")
        void escapedQuotes() throws IOException {
            Book book = importOne("9780135957059,\"The \"\"Pragmatic\"\" Programmer\",Andy Hunt,39.95");

            assertThat(book.title()).isEqualTo("The \"Pragmatic\" Programmer");
        }

        @Test
        @DisplayName("reads an empty quoted field and an empty unquoted field as empty text")
        void emptyFields() throws IOException {
            Book book = importOne("9780000000001,\"\",,10");

            assertThat(book.title()).isEmpty();
            assertThat(book.author()).isEmpty();
        }

        @Test
        @DisplayName("reads a quoted price")
        void quotedPrice() throws IOException {
            assertThat(importOne("9780134685991,Effective Java,Joshua Bloch,\"45.99\"")).isEqualTo(EFFECTIVE_JAVA);
        }

        @Test
        @DisplayName("keeps a double quote in the middle of an unquoted field")
        void quoteInsideUnquotedField() throws IOException {
            Book book = importOne("9780132350884,Clean \"Code\",Robert C. Martin,33.00");

            assertThat(book.title()).isEqualTo("Clean \"Code\"");
        }

        @Test
        @DisplayName("doesn't trim fields")
        void keepsSpaces() throws IOException {
            Book book = importOne("9780134685991, Effective Java ,Joshua Bloch,45.99");

            assertThat(book.title()).isEqualTo(" Effective Java ");
        }

        @Test
        @DisplayName("accepts a price of 0")
        void zeroPrice() throws IOException {
            assertThat(importOne("9780000000002,Free sample,,0").price()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("ignores a byte order mark at the start of the file, as Excel writes it")
        void ignoresByteOrderMark() throws IOException {
            Path file = csv("\uFEFF" + HEADER, "9780134685991,Effective Java,Joshua Bloch,45.99");

            ImportResult result = BookCsvImporter.importFrom(file);

            assertThat(result.books()).containsExactly(EFFECTIVE_JAVA);
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("reads Windows line endings (\\r\\n)")
        void windowsLineEndings() throws IOException {
            Path file = Files.writeString(dir.resolve("windows.csv"),
                    HEADER + "\r\n9780134685991,Effective Java,Joshua Bloch,45.99\r\n");

            ImportResult result = BookCsvImporter.importFrom(file);

            assertThat(result.books()).containsExactly(EFFECTIVE_JAVA);
            assertThat(result.errors()).isEmpty();
        }

        @Test
        @DisplayName("the report has only the two counts when there are no errors")
        void reportWithoutErrors() throws IOException {
            Path report = dir.resolve("report.txt");

            ImportReport.write(report, new ImportResult(List.of(EFFECTIVE_JAVA), List.of()));

            assertThat(Files.readString(report)).isEqualTo("books: 1\nerrors: 0\n");
        }

        @Test
        @DisplayName("the report replaces an existing file")
        void reportReplacesFile() throws IOException {
            Path report = Files.writeString(dir.resolve("report.txt"), "an older report\nwith more lines\n");

            ImportReport.write(report, new ImportResult(List.of(), List.of()));

            assertThat(Files.readString(report)).isEqualTo("books: 0\nerrors: 0\n");
        }

        @Test
        @DisplayName("the report is written as UTF-8")
        void reportIsUtf8() throws IOException {
            Path report = dir.resolve("report.txt");
            var result = new ImportResult(List.of(), List.of(new ImportError(2, "invalid price \"만원\"")));

            ImportReport.write(report, result);

            assertThat(Files.readString(report, StandardCharsets.UTF_8))
                    .isEqualTo("books: 0\nerrors: 1\nline 2: invalid price \"만원\"\n");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {
                "ISBN,TITLE,AUTHOR,PRICE",
                "isbn,title,price,author",
                "isbn,title,author",
                "isbn,title,author,price,stock",
                "isbn, title, author, price",
                ""
        })
        @DisplayName("rejects a file whose first line isn't the header, and reads nothing else")
        void wrongHeader(String header) throws IOException {
            Path file = csv(header, "9780134685991,Effective Java,Joshua Bloch,45.99");

            ImportResult result = BookCsvImporter.importFrom(file);

            assertThat(result.books()).isEmpty();
            assertThat(result.errors()).containsExactly(WRONG_HEADER);
        }

        @Test
        @DisplayName("reports an empty file as a missing header")
        void emptyFile() throws IOException {
            Path file = Files.writeString(dir.resolve("empty.csv"), "");

            ImportResult result = BookCsvImporter.importFrom(file);

            assertThat(result.books()).isEmpty();
            assertThat(result.errors()).containsExactly(WRONG_HEADER);
        }

        @Test
        @DisplayName("reports rows with too few or too many fields")
        void wrongNumberOfFields() throws IOException {
            Path file = csv(HEADER,
                    "9780134685991,Effective Java,45.99",
                    "9780134685991,Effective Java,Joshua Bloch,45.99,in stock");

            ImportResult result = BookCsvImporter.importFrom(file);

            assertThat(result.books()).isEmpty();
            assertThat(result.errors()).containsExactly(
                    new ImportError(2, "expected 4 fields but found 3"),
                    new ImportError(3, "expected 4 fields but found 5"));
        }

        @Test
        @DisplayName("reports a quoted field without its closing quote")
        void unclosedQuote() throws IOException {
            Path file = csv(HEADER, "9780134685991,\"Effective Java,Joshua Bloch,45.99");

            assertThat(BookCsvImporter.importFrom(file).errors())
                    .containsExactly(new ImportError(2, "quoted field is not closed"));
        }

        @Test
        @DisplayName("reports a price with a thousands separator as invalid")
        void thousandsSeparator() throws IOException {
            Path file = csv(HEADER, "9788936434120,채식주의자,한강,\"15,000\"");

            assertThat(BookCsvImporter.importFrom(file).errors())
                    .containsExactly(new ImportError(2, "invalid price \"15,000\""));
        }

        @Test
        @DisplayName("reports a negative price as invalid")
        void negativePrice() throws IOException {
            Path file = csv(HEADER, "9780134685991,Effective Java,Joshua Bloch,-45.99");

            assertThat(BookCsvImporter.importFrom(file).errors())
                    .containsExactly(new ImportError(2, "invalid price \"-45.99\""));
        }

        @Test
        @DisplayName("throws NoSuchFileException for a file that doesn't exist")
        void missingFile() {
            assertThatThrownBy(() -> BookCsvImporter.importFrom(dir.resolve("missing.csv")))
                    .isInstanceOf(NoSuchFileException.class);
        }

        @Test
        @DisplayName("throws MalformedInputException for a file that isn't UTF-8 (Korean Excel's CP949)")
        void notUtf8() throws IOException {
            String text = HEADER + "\n9788936434120,채식주의자,한강,15000\n";
            Path file = Files.write(dir.resolve("cp949.csv"), text.getBytes(Charset.forName("MS949")));

            assertThatThrownBy(() -> BookCsvImporter.importFrom(file)).isInstanceOf(MalformedInputException.class);
        }

        @Test
        @DisplayName("rejects null arguments")
        void rejectsNulls() {
            var empty = new ImportResult(List.of(), List.of());

            assertThatThrownBy(() -> BookCsvImporter.importFrom(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ImportReport.write(null, empty)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> ImportReport.write(dir.resolve("report.txt"), null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
