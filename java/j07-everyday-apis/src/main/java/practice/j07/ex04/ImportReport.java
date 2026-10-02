package practice.j07.ex04;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes a short summary of an import to a text file, for example:
 *
 * <pre>
 * books: 2
 * errors: 1
 * line 4: invalid price "12,000"
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Line 1 is {@code books: } followed by the number of books. Line 2 is {@code errors: } followed by the number
 *       of errors. Then comes one line per error, in the order of {@link ImportResult#errors()}: {@code line }, the
 *       line number, {@code : } and the message.</li>
 *   <li>Every line ends with {@code \n}, the last one too, on every operating system.</li>
 *   <li>The file is written as UTF-8. An existing file is replaced.</li>
 *   <li>Problems with the file are thrown as {@link IOException}. {@code null} arguments throw
 *       {@link NullPointerException}.</li>
 * </ul>
 */
public final class ImportReport {

    private ImportReport() {
    }

    /** Writes the summary of {@code result} to {@code target}. */
    public static void write(Path target, ImportResult result) throws IOException {
        // TODO: J07-04
        throw new UnsupportedOperationException("TODO: J07-04");
    }
}
