package practice.j03.ex01;

import java.io.Reader;
import java.nio.file.Path;
import java.util.Map;

/**
 * Loads settings written as {@code key=value} lines, like a small {@code .env} or {@code .properties} file:
 *
 * <pre>
 * # Database
 * db.url = jdbc:mysql://localhost:3306/shop
 * db.pool-size=10
 * </pre>
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Lines are numbered from 1. Every line counts, including blank lines and comments.</li>
 *   <li>A blank line (empty, or only whitespace) is skipped.</li>
 *   <li>A comment line is skipped. Its first non-whitespace character is {@code #}. A {@code #} anywhere else
 *       is part of the value: {@code color=#ff0000} has the value {@code #ff0000}.</li>
 *   <li>Every other line is split at its <em>first</em> {@code =}. The key is the text before it and the value is
 *       everything after it, so a value may contain {@code =}. Both are trimmed with {@link String#strip()}.</li>
 *   <li>The value may be empty ({@code name=}), but the key may not.</li>
 *   <li>Keys are case-sensitive: {@code Port} and {@code port} are different keys.</li>
 * </ul>
 *
 * <h2>Errors</h2>
 * Every problem is reported as a {@link ConfigException}, so callers have only one exception type to handle.
 * <ul>
 *   <li>A line without {@code =}, or with an empty key, is malformed. The message contains {@code line N}, where
 *       N is the line number: for example {@code "line 3: expected key=value"}.</li>
 *   <li>A key that appears twice is an error, too, because silently keeping one of the two values would hide a
 *       copy-paste mistake. The message contains the key and {@code line N} for the second occurrence.</li>
 *   <li>An {@link java.io.IOException} while reading or closing becomes a {@code ConfigException} with the
 *       {@code IOException} as its cause.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}.</li>
 * </ul>
 */
public class ConfigLoader {

    /**
     * Reads every line from {@code reader} and returns the settings as an unmodifiable map whose keys are in the
     * order they appear in the text. Empty text gives an empty map.
     *
     * <p>This method takes ownership of the reader: it always closes it, also when it throws.
     */
    public Map<String, String> load(Reader reader) {
        // TODO: J03-01
        throw new UnsupportedOperationException("TODO: J03-01");
    }

    /**
     * Reads the file at {@code path} as UTF-8, with the same rules as {@link #load(Reader)}, and closes it.
     *
     * <p>If the file can't be opened or read, this throws a {@link ConfigException} whose message contains the path
     * and whose cause is the original {@link java.io.IOException}. For a file that doesn't exist, that cause is a
     * {@link java.nio.file.NoSuchFileException}.
     */
    public Map<String, String> loadFile(Path path) {
        // TODO: J03-01
        throw new UnsupportedOperationException("TODO: J03-01");
    }
}
