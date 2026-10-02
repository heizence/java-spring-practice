package practice.j09.ex03;

import java.time.Duration;
import java.util.Objects;

/**
 * One measured call: the method's name and how long the call took. Given code: you don't need to change it.
 *
 * @param method  the method's name, such as {@code "findById"}
 * @param elapsed how long the call took
 */
public record Timing(String method, Duration elapsed) {

    public Timing {
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(elapsed, "elapsed");
    }
}
