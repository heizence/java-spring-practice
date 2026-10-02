package practice.j03.ex01;

/**
 * Thrown when settings can't be loaded: the text is malformed, or the file can't be read.
 *
 * <p>It's <em>unchecked</em> (it extends {@link RuntimeException}). The code that asks for a setting can't repair a
 * broken configuration file, so making every caller catch this exception would only add noise. Usually the
 * application stops at startup and shows this exception's message.
 *
 * <p>This class is given. In exercise J03-01 you build {@link ConfigLoader}.
 */
public class ConfigException extends RuntimeException {

    /** For a problem that has no underlying exception, such as a malformed line. */
    public ConfigException(String message) {
        super(message);
    }

    /** For a problem that another exception caused, such as an {@link java.io.IOException}. Keeps it as the cause. */
    public ConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}
