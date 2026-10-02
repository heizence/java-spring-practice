package practice.j07.ex04;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Imports books from a CSV file, such as one exported from a spreadsheet:
 *
 * <pre>
 * isbn,title,author,price
 * 9788936434120,채식주의자,한강,15000
 * 9780134685991,"Effective Java, 3rd Edition",Joshua Bloch,45.99
 * </pre>
 *
 * <h2>The file</h2>
 * <ul>
 *   <li>The file is read as UTF-8. A byte order mark at the very start of the file is ignored: that's the invisible
 *       character U+FEFF that Excel writes at the start of "CSV UTF-8" files.</li>
 *   <li>Lines end with {@code \n} or {@code \r\n}. They're numbered from 1, and every line counts: the header,
 *       the rows and blank lines.</li>
 *   <li>Line 1 must be exactly {@value #HEADER}. If it isn't, or the file is empty, the result has no books and
 *       one error for line 1, and the rest of the file isn't read.</li>
 * </ul>
 *
 * <h2>Rows</h2>
 * <ul>
 *   <li>After the header, blank lines (empty, or only whitespace) are skipped. Every other line is one book.</li>
 *   <li>Fields are separated by commas. They aren't trimmed: spaces are part of the value.</li>
 *   <li>A field that starts with a double quote ({@code "}) is <em>quoted</em>. It ends at the next double quote that
 *       isn't doubled. Inside it, a comma is part of the value, and two double quotes ({@code ""}) stand for one.
 *       A quoted field ends on the same line.</li>
 *   <li>Anywhere else, a double quote is a normal character, and so is any text between a closing quote and the
 *       next comma.</li>
 *   <li>A row has exactly 4 fields: isbn, title, author and price. The author may be empty.</li>
 *   <li>The price is a number such as {@code 15000} or {@code 45.99}, in the format that
 *       {@code new BigDecimal(String)} reads, and it isn't negative.</li>
 * </ul>
 *
 * <h2>Errors</h2>
 * A row with a problem doesn't stop the import: it becomes an {@link ImportError} with its line number, and the
 * other rows are still imported. Books and errors are listed in file order. The messages are exactly:
 * <ul>
 *   <li>{@code header must be "isbn,title,author,price"} for a missing or wrong header (line 1),</li>
 *   <li>{@code quoted field is not closed} for a quoted field without its closing quote,</li>
 *   <li>{@code expected 4 fields but found 3} for a row with another number of fields (here, 3),</li>
 *   <li>{@code invalid price "12,000"} for a price that isn't a number or is negative (here, {@code 12,000}).</li>
 * </ul>
 * Problems with the file itself are thrown as {@link IOException}: for example
 * {@link java.nio.file.NoSuchFileException} for a missing file, or {@link java.nio.charset.MalformedInputException}
 * for a file that isn't UTF-8, such as a CSV that Korean Excel saved in CP949. A {@code null} path throws
 * {@link NullPointerException}.
 */
public final class BookCsvImporter {

    /** The required first line of the file. */
    public static final String HEADER = "isbn,title,author,price";

    private BookCsvImporter() {
    }

    /** Reads {@code file} and returns the books in it and the problems it found. */
    public static ImportResult importFrom(Path file) throws IOException {
        // TODO: J07-04
        throw new UnsupportedOperationException("TODO: J07-04");
    }
}
