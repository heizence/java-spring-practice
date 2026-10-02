package practice.j07.ex04;

import java.io.BufferedReader;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    private static final char BYTE_ORDER_MARK = '\uFEFF';
    private static final int FIELD_COUNT = 4;

    private BookCsvImporter() {
    }

    /** Reads {@code file} and returns the books in it and the problems it found. */
    public static ImportResult importFrom(Path file) throws IOException {
        Objects.requireNonNull(file, "file");
        List<Book> books = new ArrayList<>();
        List<ImportError> errors = new ArrayList<>();
        // A BufferedReader reads one line at a time, so a large file never has to fit into memory, and readLine()
        // removes both \n and \r\n. Files.newBufferedReader reports bytes that aren't valid UTF-8 as a
        // MalformedInputException instead of quietly turning them into '?'.
        // IOException isn't caught here: the caller (a controller or a batch job) decides what a missing file means.
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String header = reader.readLine(); // null for an empty file
            if (header != null && !header.isEmpty() && header.charAt(0) == BYTE_ORDER_MARK) {
                header = header.substring(1);
            }
            if (!HEADER.equals(header)) {
                return new ImportResult(List.of(), List.of(new ImportError(1, "header must be \"" + HEADER + "\"")));
            }

            int lineNumber = 1;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                lineNumber++; // count every line, blank ones too, so the numbers match what an editor shows
                if (line.isBlank()) {
                    continue;
                }
                try {
                    books.add(parseBook(line));
                } catch (InvalidRowException e) {
                    errors.add(new ImportError(lineNumber, e.getMessage()));
                }
            }
        }
        return new ImportResult(books, errors);
    }

    private static Book parseBook(String line) throws InvalidRowException {
        List<String> fields = splitFields(line);
        if (fields.size() != FIELD_COUNT) {
            throw new InvalidRowException("expected " + FIELD_COUNT + " fields but found " + fields.size());
        }
        return new Book(fields.get(0), fields.get(1), fields.get(2), parsePrice(fields.get(3)));
    }

    /**
     * Splits one line into its fields, following the quoting rules in the class Javadoc. {@code line.split(",")}
     * can't do this: it splits inside quotes, and it even drops empty fields at the end of the line.
     */
    private static List<String> splitFields(String line) throws InvalidRowException {
        List<String> fields = new ArrayList<>();
        var field = new StringBuilder();
        int fieldStart = 0; // the index where the current field begins
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (quoted) {
                if (c != '"') {
                    field.append(c);
                } else {
                    quoted = false; // the closing quote
                }
            } else if (c == ',') {
                fields.add(field.toString());
                field.setLength(0);
                fieldStart = i + 1;
            } else if (c == '"' && i == fieldStart) {
                quoted = true; // a quote opens a quoted field only as the field's first character
            } else {
                field.append(c);
            }
        }
        if (quoted) {
            throw new InvalidRowException("quoted field is not closed");
        }
        fields.add(field.toString()); // the last field has no comma after it
        return fields;
    }

    private static BigDecimal parsePrice(String text) throws InvalidRowException {
        BigDecimal price;
        try {
            // Java has no "try parse": catching NumberFormatException is the normal way to validate a number.
            price = new BigDecimal(text);
        } catch (NumberFormatException e) {
            throw new InvalidRowException("invalid price \"" + text + "\"", e);
        }
        if (price.signum() < 0) {
            throw new InvalidRowException("invalid price \"" + text + "\"");
        }
        return price;
    }

    /**
     * A row that can't be imported. It's a <em>checked</em> exception on purpose: the compiler makes sure that the
     * import loop handles every invalid row, and as a private class it never leaks out of this file.
     */
    private static final class InvalidRowException extends Exception {

        InvalidRowException(String message) {
            super(message);
        }

        InvalidRowException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
