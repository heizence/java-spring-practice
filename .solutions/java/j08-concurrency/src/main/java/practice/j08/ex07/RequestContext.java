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
 *   <li>Once the action has returned or thrown, its id is no longer current. An exception from the action reaches
 *       the caller unchanged.</li>
 *   <li>The id belongs to the thread that runs the action. Other threads don't see it, not even threads that the
 *       action starts itself.</li>
 *   <li>A {@code null} id or action throws {@link NullPointerException}, and a blank id throws
 *       {@link IllegalArgumentException}. In both cases the action doesn't run.</li>
 * </ul>
 */
public final class RequestContext {

    // One key for the whole application. The value bound to it belongs to one thread and one scope.
    private static final ScopedValue<String> REQUEST_ID = ScopedValue.newInstance();

    private RequestContext() {
    }

    /** Runs {@code action} with {@code requestId} as the current request id. */
    public static void runWithRequestId(String requestId, Runnable action) {
        Objects.requireNonNull(action, "action");
        // where(...).run(...) binds the value, runs the action and unbinds the value again, also when the action
        // throws. A nested call binds a new value for its own scope; when it ends, the outer value is back.
        ScopedValue.where(REQUEST_ID, checked(requestId)).run(action);
    }

    /** Calls {@code action} with {@code requestId} as the current request id, and returns its result. */
    public static <T> T callWithRequestId(String requestId, Supplier<T> action) {
        Objects.requireNonNull(action, "action");
        // call() takes a ScopedValue.CallableOp, whose call() may throw a checked exception. action::get only throws
        // unchecked exceptions, so this method needs no throws clause.
        return ScopedValue.where(REQUEST_ID, checked(requestId)).call(action::get);
    }

    /** The id of the request that the calling code is working on, or empty outside of a request. */
    public static Optional<String> currentRequestId() {
        // get() throws NoSuchElementException when nothing is bound, so ask isBound() first.
        // (orElse(null) is no shortcut: since Java 25 it throws NullPointerException for a null argument.)
        return REQUEST_ID.isBound() ? Optional.of(REQUEST_ID.get()) : Optional.empty();
    }

    private static String checked(String requestId) {
        Objects.requireNonNull(requestId, "requestId");
        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        return requestId;
    }
}
