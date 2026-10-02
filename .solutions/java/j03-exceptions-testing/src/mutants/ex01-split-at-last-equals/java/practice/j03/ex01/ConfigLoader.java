package practice.j03.ex01;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

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
 *   <li>If closing fails after an earlier failure (a malformed line, or an {@code IOException} while reading),
 *       the earlier failure is reported as described above. The {@code IOException} from closing isn't lost: it
 *       is attached to the earlier exception as a <em>suppressed</em> exception
 *       ({@link Throwable#getSuppressed()}).</li>
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
        Objects.requireNonNull(reader, "reader");
        // try-with-resources closes the reader on every path: after a normal return, after a ConfigException for a
        // malformed line, and after an IOException. If close() fails too, its exception is attached to the first
        // one as "suppressed" instead of replacing it. (Closing the BufferedReader closes the reader inside it.)
        try (var lines = new BufferedReader(reader)) {
            return parse(lines);
        } catch (IOException e) {
            // Only IOException is caught here. A ConfigException from parse() is unchecked and passes through.
            throw new ConfigException("could not read the settings", e);
        }
    }

    /**
     * Reads the file at {@code path} as UTF-8, with the same rules as {@link #load(Reader)}, and closes it.
     *
     * <p>If the file can't be opened or read, this throws a {@link ConfigException} whose message contains the path
     * and whose cause is the original {@link java.io.IOException}. For a file that doesn't exist, that cause is a
     * {@link java.nio.file.NoSuchFileException}.
     */
    public Map<String, String> loadFile(Path path) {
        Objects.requireNonNull(path, "path");
        try (BufferedReader lines = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return parse(lines);
        } catch (IOException e) {
            // Translate the checked IOException into our unchecked exception and keep it as the cause. The stack
            // trace then shows both what we were doing (reading this file) and what went wrong underneath.
            throw new ConfigException("could not read the settings file " + path, e);
        }
    }

    /** The parsing rules, shared by both public methods. The caller opened the reader, so the caller closes it. */
    private static Map<String, String> parse(BufferedReader lines) throws IOException {
        Map<String, String> settings = new LinkedHashMap<>(); // keeps the keys in insertion order; HashMap doesn't
        int lineNumber = 0;
        for (String line = lines.readLine(); line != null; line = lines.readLine()) {
            lineNumber++; // count every line, so the number matches what an editor shows
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int equals = trimmed.lastIndexOf('=');
            if (equals < 0) {
                // Don't repeat the line in the message: settings often hold passwords, and messages end up in logs.
                throw new ConfigException("line " + lineNumber + ": expected key=value");
            }
            String key = trimmed.substring(0, equals).strip();
            String value = trimmed.substring(equals + 1).strip();
            if (key.isEmpty()) {
                throw new ConfigException("line " + lineNumber + ": the key is empty");
            }
            if (settings.containsKey(key)) {
                throw new ConfigException("line " + lineNumber + ": duplicate key '" + key + "'");
            }
            settings.put(key, value);
        }
        // Nobody else has a reference to `settings`, so a read-only view is enough; no copy needed.
        return Collections.unmodifiableMap(settings);
    }
}
