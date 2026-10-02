package practice.j09.ex03;

import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Two dynamic proxies built with {@link java.lang.reflect.Proxy}: the mechanism behind Spring's
 * {@code @Transactional}, {@code @Cacheable} and {@code @Async}.
 *
 * <h2>Rules for both proxies</h2>
 * <ul>
 *   <li>{@code type} must be an interface, because a JDK proxy can only implement interfaces. (For classes, Spring
 *       generates a subclass with CGLIB instead.) Otherwise: {@link IllegalArgumentException}.</li>
 *   <li>{@code null} arguments throw {@link NullPointerException}.</li>
 *   <li>Every call is forwarded to {@code target}, and its result is returned. When the target throws, the caller
 *       gets that same exception, not an {@link java.lang.reflect.InvocationTargetException} or
 *       {@link java.lang.reflect.UndeclaredThrowableException}.</li>
 *   <li>{@code equals}, {@code hashCode} and {@code toString} are never timed and never run in a transaction. A proxy
 *       is equal only to itself, its {@code hashCode} is {@link System#identityHashCode} of the proxy, and its
 *       {@code toString} returns the target's {@code toString()}.</li>
 * </ul>
 */
public final class Proxies {

    private Proxies() {
    }

    /**
     * Returns a proxy that measures every call. It reads {@code ticker} (a clock in nanoseconds, such as
     * {@code System::nanoTime}) before and after the call, and passes a {@link Timing} with the method's name and the
     * difference to {@code log}. A call that throws is measured and logged too.
     */
    public static <T> T timing(Class<T> type, T target, LongSupplier ticker, Consumer<Timing> log) {
        // TODO: J09-03
        throw new UnsupportedOperationException("TODO: J09-03");
    }

    /**
     * Returns a proxy that runs the methods annotated with {@link Transactional} in a transaction.
     *
     * <ol>
     *   <li>The annotation counts on the interface's method and on the target class's method that implements it.</li>
     *   <li>Before the call: {@link TxManager#begin()}.</li>
     *   <li>When the call returns normally: {@link TxManager#commit()}.</li>
     *   <li>When it throws a {@link RuntimeException} or an {@link Error}: {@link TxManager#rollback()}, then the
     *       exception reaches the caller.</li>
     *   <li>When it throws a checked exception: {@link TxManager#commit()}, then the exception reaches the caller.
     *       That's Spring's default rule: a checked exception counts as an expected business outcome, such as
     *       "insufficient funds".</li>
     * </ol>
     *
     * <p>Methods without the annotation are forwarded to the target without a transaction.
     */
    public static <T> T transactional(Class<T> type, T target, TxManager txManager) {
        // TODO: J09-03
        throw new UnsupportedOperationException("TODO: J09-03");
    }
}
