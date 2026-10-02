package practice.j07.ex04;

import java.util.Objects;

/**
 * A line of an import file that couldn't be imported.
 *
 * @param lineNumber the line in the file, counting from 1
 * @param message    what's wrong, for example {@code invalid price "12,000"}
 */
public record ImportError(int lineNumber, String message) {

    public ImportError {
        Objects.requireNonNull(message, "message");
    }
}
