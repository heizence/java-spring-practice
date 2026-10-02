package practice.j08.ex07;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Makes the id of the current request available to all code that runs while the request is handled (logging,
 * auditing, calls to other services), without passing it as a parameter through every method.
 *
 * <p>Use a {@link ScopedValue} (final since Java 25), not a {@link ThreadLocal}: a scoped value is bound only while
 * one piece of code runs, so it can't leak into the next task of a pooled thread.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #runWithRequestId} runs {@code action} with {@code requestId} as the current request id.
 *       {@link #callWithRequestId} does the same for an action that returns a value, and returns that value.</li>
 *   <li>{@link #currentRequestId()} returns the current request id, or empty when the calling code doesn't run
 *       inside one of these two methods.</li>
 *   <li>Calls can be nested. Inside the inner call, the inner id is current. After it, the outer id is current
 *       again.</li>
 *   <li>Once the action has returned or thrown, its id is no longer current.</li>
 *   <li>The id belongs to the thread that runs the action. Other threads don't see it, not even threads that the
 *       action starts itself.</li>
 *   <li>A {@code null} id or action throws {@link NullPointerException}, and a blank id throws
 *       {@link IllegalArgumentException}. In both cases the action doesn't run.</li>
 * </ul>
 */
public final class RequestContext {

    // The id of the request that is being handled.
    private static volatile String currentId;

    private RequestContext() {
    }

    /** Runs {@code action} with {@code requestId} as the current request id. */
    public static void runWithRequestId(String requestId, Runnable action) {
        Objects.requireNonNull(action, "action");
        String previous = currentId;
        currentId = checked(requestId);
        try {
            action.run();
        } finally {
            currentId = previous;
        }
    }

    /** Calls {@code action} with {@code requestId} as the current request id, and returns its result. */
    public static <T> T callWithRequestId(String requestId, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        String previous = currentId;
        currentId = checked(requestId);
        try {
            return action.get();
        } finally {
            currentId = previous;
        }
    }

    /** The id of the request that the calling code is working on, or empty outside of a request. */
    public static Optional<String> currentRequestId() {
        return Optional.ofNullable(currentId);
    }

    private static String checked(String requestId) {
        Objects.requireNonNull(requestId, "requestId");
        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        return requestId;
    }
}
