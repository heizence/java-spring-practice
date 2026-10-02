package practice.j09.ex03;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Objects;
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

    public static <T> T timing(Class<T> type, T target, LongSupplier ticker, Consumer<Timing> log) {
        requireInterface(type);
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(ticker, "ticker");
        Objects.requireNonNull(log, "log");
        InvocationHandler handler = (proxy, method, args) -> {
            if (isObjectMethod(method)) {
                return objectMethod(proxy, method, args, target);
            }
            long start = ticker.getAsLong();
            try {
                return forward(target, method, args);
            } finally {
                // finally: a call that throws took time too, and slow failures are exactly what you want to see.
                log.accept(new Timing(method.getName(), Duration.ofNanos(ticker.getAsLong() - start)));
            }
        };
        return newProxy(type, handler);
    }

    public static <T> T transactional(Class<T> type, T target, TxManager txManager) {
        requireInterface(type);
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(txManager, "txManager");
        InvocationHandler handler = (proxy, method, args) -> {
            if (isObjectMethod(method)) {
                return objectMethod(proxy, method, args, target);
            }
            if (!isTransactional(method, target)) {
                return forward(target, method, args);
            }
            txManager.begin();
            Object result;
            try {
                result = forward(target, method, args);
            } catch (RuntimeException | Error e) {
                txManager.rollback();
                throw e;
            } catch (Throwable checked) {
                // Spring's default rule: a checked exception is an expected outcome, so the work so far is committed.
                // (In Spring, @Transactional(rollbackFor = Exception.class) changes that.)
                txManager.commit();
                throw checked; // allowed: the interface method declares it, or the target couldn't have thrown it
            }
            txManager.commit();
            return result;
        };
        return newProxy(type, handler);
    }

    /** Calls the target and unwraps the exception it throws. */
    private static Object forward(Object target, Method method, Object[] args) throws Throwable {
        try {
            // args is null for a method without parameters; invoke() accepts that.
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            // Reflection wraps whatever the target threw. Throwing the wrapper would reach the caller as an
            // UndeclaredThrowableException, because no interface method declares InvocationTargetException.
            throw e.getCause();
        }
    }

    private static boolean isTransactional(Method interfaceMethod, Object target) throws NoSuchMethodException {
        return interfaceMethod.isAnnotationPresent(Transactional.class);
    }

    private static boolean isObjectMethod(Method method) {
        // For equals, hashCode and toString, Proxy passes the Method objects of java.lang.Object.
        return method.getDeclaringClass() == Object.class;
    }

    private static Object objectMethod(Object proxy, Method method, Object[] args, Object target) {
        return switch (method.getName()) {
            // Forwarding equals to the target would make proxy.equals(proxy) false (the target doesn't know the
            // proxy), and a proxy in a HashSet could never be found again.
            case "equals" -> proxy == args[0];
            case "hashCode" -> System.identityHashCode(proxy);
            default -> target.toString();
        };
    }

    private static <T> T newProxy(Class<T> type, InvocationHandler handler) {
        // The class loader of the interface can see the interface, so it can define the proxy class.
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[] {type}, handler));
    }

    private static void requireInterface(Class<?> type) {
        Objects.requireNonNull(type, "type");
        if (!type.isInterface()) {
            throw new IllegalArgumentException(type.getName() + " is not an interface. JDK proxies can only "
                    + "implement interfaces; Spring creates a CGLIB subclass for classes.");
        }
    }
}
