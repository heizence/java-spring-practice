package practice.j04.ex03;

import java.util.Optional;
import java.util.Set;

/**
 * A map from a type to one value of that type, with type-safe access:
 *
 * <pre>
 * var context = new TypedMap();
 * context.put(String.class, "Minji");
 * context.put(Integer.class, 42);
 * Optional&lt;String&gt; name = context.get(String.class);   // no cast needed
 * </pre>
 *
 * <p>Each value can have a different type, so a plain {@code Map<Class<?>, Object>} would force a cast on every
 * {@code get}. Effective Java calls this pattern a <em>typesafe heterogeneous container</em>, and Guava's
 * {@code ClassToInstanceMap} is one.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Keys are compared exactly. A value stored under {@code Number.class} is not found with
 *       {@code Integer.class}, and the other way round.</li>
 *   <li>{@link #put} replaces any value stored earlier for the same type.</li>
 *   <li>{@link #put} checks at runtime that the value really is an instance of the type, with
 *       {@link Class#cast}. Code that bypasses the compiler's checks, for example with a raw {@code Class},
 *       gets a {@link ClassCastException} right away, and the map stays unchanged.</li>
 *   <li>{@code null} types and {@code null} values throw {@link NullPointerException}.</li>
 *   <li>A primitive type such as {@code int.class} throws {@link IllegalArgumentException} in every method:
 *       a primitive type has no instances, so no value could ever be stored for it. Use the wrapper type, such
 *       as {@code Integer.class}, instead. Array types such as {@code int[].class} are not primitive and are
 *       fine.</li>
 * </ul>
 */
public final class TypedMap {

    /** Stores {@code value} for {@code type}, replacing any earlier value for that type. */
    public <T> void put(Class<T> type, T value) {
        // TODO: J04-03
        throw new UnsupportedOperationException("TODO: J04-03");
    }

    /** Returns the value stored for {@code type}, or an empty {@code Optional} if there is none. */
    public <T> Optional<T> get(Class<T> type) {
        // TODO: J04-03
        throw new UnsupportedOperationException("TODO: J04-03");
    }

    /** Returns whether a value is stored for {@code type}. */
    public boolean contains(Class<?> type) {
        // TODO: J04-03
        throw new UnsupportedOperationException("TODO: J04-03");
    }

    /** Removes the value stored for {@code type} and returns it, or returns empty if there was none. */
    public <T> Optional<T> remove(Class<T> type) {
        // TODO: J04-03
        throw new UnsupportedOperationException("TODO: J04-03");
    }

    /**
     * Returns the types that have a value, in no particular order, as an unmodifiable snapshot: later changes to
     * the map don't show up in a set you already have.
     */
    public Set<Class<?>> types() {
        // TODO: J04-03
        throw new UnsupportedOperationException("TODO: J04-03");
    }
}
