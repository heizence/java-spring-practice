package practice.j04.ex03;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

    // The compiler can't express "the value's type matches its key", so the map stores plain Objects.
    // The methods below are the only way in and out, and they keep that promise themselves.
    private final Map<Class<?>, Object> values = new HashMap<>();

    /** Stores {@code value} for {@code type}, replacing any earlier value for that type. */
    public <T> void put(Class<T> type, T value) {
        checkType(type);
        Objects.requireNonNull(value, "value");
        // type.cast checks the value at runtime. Normally the compiler has already done that, but raw types and
        // unchecked casts can bypass it, and then we want to fail here, not in some later get().
        values.put(type, type.cast(value));
    }

    /** Returns the value stored for {@code type}, or an empty {@code Optional} if there is none. */
    public <T> Optional<T> get(Class<T> type) {
        checkType(type);
        // The map returns an Object. type.cast turns it into a T with a runtime check, so there's no unchecked
        // (T) cast and no compiler warning. Class.cast(null) returns null, which ofNullable turns into empty.
        return Optional.ofNullable(type.cast(values.get(type)));
    }

    /** Returns whether a value is stored for {@code type}. */
    public boolean contains(Class<?> type) {
        checkType(type);
        return values.containsKey(type);
    }

    /** Removes the value stored for {@code type} and returns it, or returns empty if there was none. */
    public <T> Optional<T> remove(Class<T> type) {
        checkType(type);
        return Optional.ofNullable(type.cast(values.remove(type)));
    }

    /**
     * Returns the types that have a value, in no particular order, as an unmodifiable snapshot: later changes to
     * the map don't show up in a set you already have.
     */
    public Set<Class<?>> types() {
        return Set.copyOf(values.keySet());
    }

    private static void checkType(Class<?> type) {
        Objects.requireNonNull(type, "type");
    }
}
