package practice.j09.ex01;

import java.util.Map;
import java.util.SequencedMap;

/**
 * A small reflection toolkit, like the helpers inside frameworks, test libraries and debuggers.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code null} target, type, method name or argument array throws {@link NullPointerException}.</li>
 *   <li>{@link #newInstance} and {@link #invoke} only consider <em>public</em> constructors and methods. They choose
 *       one by its parameters: the same number as there are arguments, and each argument must fit its parameter
 *       type. A primitive parameter accepts its wrapper class ({@code int} accepts an {@code Integer}), but there is
 *       no widening ({@code long} doesn't accept an {@code Integer}). A {@code null} argument fits any parameter
 *       that isn't primitive.</li>
 *   <li>Exactly one constructor or method must fit. None, or more than one, throws
 *       {@link IllegalArgumentException} with a message that names the class (and, for {@link #invoke}, the
 *       method).</li>
 *   <li>When the called constructor or method throws, the caller gets that same exception if it is a
 *       {@link RuntimeException} or an {@link Error}. A checked exception arrives wrapped in an
 *       {@link java.lang.reflect.UndeclaredThrowableException}, as with Java's dynamic proxies.</li>
 * </ul>
 */
public final class Inspector {

    private Inspector() {
    }

    /**
     * The values of the instance fields declared in the object's own class, of any access level, in declaration
     * order.
     *
     * <ul>
     *   <li>Static fields, the fields of superclasses, and <em>synthetic</em> fields are left out. Synthetic fields
     *       are fields the compiler adds, such as {@code this$0}, the reference to the outer object in an inner
     *       class.</li>
     *   <li>Primitive values come back boxed ({@code 3} as an {@code Integer}). {@code null} values are kept.</li>
     *   <li>Returns a new map that the caller may change.</li>
     *   <li>The module system protects the private fields of JDK classes such as {@link String}. For those, the
     *       {@link java.lang.reflect.InaccessibleObjectException} from {@code setAccessible} reaches the caller.</li>
     * </ul>
     */
    public static SequencedMap<String, Object> fieldValues(Object target) {
        // TODO: J09-01
        throw new UnsupportedOperationException("TODO: J09-01");
    }

    /**
     * Creates an object with the public constructor of {@code type} that fits {@code args} (see the class rules).
     *
     * @throws IllegalArgumentException if no public constructor fits, if several fit, or if {@code type} is
     *                                  abstract
     */
    public static <T> T newInstance(Class<T> type, Object... args) {
        // TODO: J09-01
        throw new UnsupportedOperationException("TODO: J09-01");
    }

    /**
     * Calls the public method {@code methodName} of {@code target} that fits {@code args} (see the class rules), and
     * returns its result: {@code null} for a {@code void} method, a boxed value for a primitive one.
     *
     * <p>Public methods inherited from superclasses count. <em>Bridge methods</em> don't: the compiler adds them
     * when a class implements a generic interface (for example {@code compareTo(Object)} next to
     * {@code compareTo(Ticket)}), and they would make every such call look ambiguous.
     *
     * @throws IllegalArgumentException if no public method of that name fits, or if several fit
     */
    public static Object invoke(Object target, String methodName, Object... args) {
        // TODO: J09-01
        throw new UnsupportedOperationException("TODO: J09-01");
    }

    // ---- Given helper: you may use it as it is. ------------------------------------------------------------------

    private static final Map<Class<?>, Class<?>> WRAPPERS = Map.of(
            boolean.class, Boolean.class, byte.class, Byte.class, char.class, Character.class,
            short.class, Short.class, int.class, Integer.class, long.class, Long.class,
            float.class, Float.class, double.class, Double.class);

    /** The wrapper class for a primitive type ({@code int.class} → {@code Integer.class}); other types unchanged. */
    private static Class<?> boxed(Class<?> type) {
        return WRAPPERS.getOrDefault(type, type);
    }
}
