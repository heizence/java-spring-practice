package practice.j06.ex01;

import java.util.Objects;
import java.util.function.Function;

/**
 * A function of three arguments. The JDK stops at two ({@link java.util.function.BiFunction}), so a project that
 * needs more declares its own functional interface, like this one.
 *
 * <p>{@code @FunctionalInterface} asks the compiler to check that the interface has exactly one abstract method,
 * so lambdas and method references can implement it. Default methods don't count.
 *
 * @param <A> type of the first argument
 * @param <B> type of the second argument
 * @param <C> type of the third argument
 * @param <R> type of the result
 */
@FunctionalInterface
public interface TriFunction<A, B, C, R> {

    /** Applies this function to the three arguments. */
    R apply(A a, B b, C c);

    default <V> TriFunction<A, B, C, V> andThen(Function<? super R, ? extends V> after) {
        Objects.requireNonNull(after, "after");
        // Inside a default method, apply(...) calls this function: the lambda captures `this`.
        return (a, b, c) -> after.apply(apply(a, b, c));
    }
}
