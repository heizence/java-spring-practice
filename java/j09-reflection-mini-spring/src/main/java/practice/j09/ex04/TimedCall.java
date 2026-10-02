package practice.j09.ex04;

import java.time.Duration;
import java.util.Objects;

/**
 * Optional step 5: one measured call of a {@link Timed} method. Given code: you don't need to change it.
 *
 * @param bean    the simple name of the bean's class, such as {@code "TimedGreeter"}
 * @param method  the method's name, such as {@code "greet"}
 * @param elapsed how long the call took
 */
public record TimedCall(String bean, String method, Duration elapsed) {

    public TimedCall {
        Objects.requireNonNull(bean, "bean");
        Objects.requireNonNull(method, "method");
        Objects.requireNonNull(elapsed, "elapsed");
    }
}
