package practice.j04.ex01;

import java.util.function.Function;

/**
 * Two values that belong together, such as a book title and its price: {@code new Pair<>("Clean Code", 3000)}
 * is a {@code Pair<String, Integer>}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Neither value may be {@code null}: the constructor throws {@link NullPointerException}.</li>
 *   <li>A pair never changes. {@link #swap()}, {@link #mapFirst} and {@link #mapSecond} return a new pair.</li>
 *   <li>As a record, a pair gets {@code equals}, {@code hashCode} and {@code toString} for free: two pairs with
 *       equal values are equal.</li>
 * </ul>
 *
 * @param first  the first value
 * @param second the second value
 * @param <A>    the type of the first value
 * @param <B>    the type of the second value
 */
public record Pair<A, B>(A first, B second) {

    /** Checks the values. The record assigns the fields after this block. */
    public Pair {
        // TODO: J04-01
        throw new UnsupportedOperationException("TODO: J04-01");
    }

    /** Returns a new pair with the two values exchanged: {@code (1, "one")} becomes {@code ("one", 1)}. */
    public Pair<B, A> swap() {
        // TODO: J04-01
        throw new UnsupportedOperationException("TODO: J04-01");
    }

    /**
     * Returns a new pair whose first value is {@code mapper} applied to this pair's first value. The second
     * value stays the same. The mapper may change the type: {@code ("Java", 25).mapFirst(String::length)} is
     * {@code (4, 25)}.
     *
     * @throws NullPointerException if {@code mapper} is {@code null} or returns {@code null}
     */
    public <R> Pair<R, B> mapFirst(Function<? super A, ? extends R> mapper) {
        // TODO: J04-01
        throw new UnsupportedOperationException("TODO: J04-01");
    }

    /**
     * Returns a new pair whose second value is {@code mapper} applied to this pair's second value. The first
     * value stays the same.
     *
     * @throws NullPointerException if {@code mapper} is {@code null} or returns {@code null}
     */
    public <R> Pair<A, R> mapSecond(Function<? super B, ? extends R> mapper) {
        // TODO: J04-01
        throw new UnsupportedOperationException("TODO: J04-01");
    }
}
