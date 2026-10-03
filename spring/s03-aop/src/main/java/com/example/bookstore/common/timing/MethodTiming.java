package com.example.bookstore.common.timing;

import java.time.Duration;
import java.util.Objects;

/**
 * One measured call of a {@link Timed} method.
 *
 * @param name    what was called: the name given in {@link Timed#value()}, or {@code ClassName.methodName}
 * @param elapsed how long the call took, including the time until it threw
 * @param failed  {@code true} if the call ended with an exception
 */
public record MethodTiming(String name, Duration elapsed, boolean failed) {

    public MethodTiming {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(elapsed, "elapsed");
    }
}
