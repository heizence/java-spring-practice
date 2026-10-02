package practice.j09.ex01;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.SequencedMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A small reflection toolkit, like the helpers inside frameworks, test libraries and debuggers.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code null} target, type, record, method name or argument array throws {@link NullPointerException}.</li>
 *   <li>{@link #newInstance} and {@link #invoke} only consider <em>public</em> constructors and methods. They choose
 *       one by its parameters: the same number as there are arguments, and each argument must fit its parameter
 *       type. A primitive parameter accepts its wrapper class ({@code int} accepts an {@code Integer}), but there is
 *       no widening ({@code long} doesn't accept an {@code Integer}). A {@code null} argument fits any parameter
 *       that isn't primitive.</li>
 *   <li>Exactly one constructor or method must fit. None, or more than one, throws
 *       {@link IllegalArgumentException} with a message that names the class.</li>
 *   <li>When the called constructor or method throws, the caller gets that same exception if it is a
 *       {@link RuntimeException} or an {@link Error}. A checked exception arrives wrapped in an
 *       {@link java.lang.reflect.UndeclaredThrowableException}, as with Java's dynamic proxies.</li>
 * </ul>
 */
public final class Inspector {

    private Inspector() {
    }

    public static SequencedMap<String, Object> fieldValues(Object target) {
        Objects.requireNonNull(target, "target");
        // A plain loop into a LinkedHashMap: Collectors.toMap would throw NullPointerException on a null value.
        SequencedMap<String, Object> values = new LinkedHashMap<>();
        // getDeclaredFields() returns every access level, but only this class's own fields. Its Javadoc promises no
        // order; HotSpot returns declaration order, and debuggers and serializers rely on that in practice.
        for (Field field : target.getClass().getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            // Fine for our own classes. For a field of a module that isn't opened to us (java.base, for example),
            // this throws InaccessibleObjectException, which the rules let through.
            field.setAccessible(true);
            values.put(field.getName(), read(field, target));
        }
        return values;
    }

    public static <T> T newInstance(Class<T> type, Object... args) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(args, "args");
        // getConstructors() returns only the public ones. Its type is Constructor<?>[], because Java can't create
        // generic arrays, so the result needs a cast below.
        Constructor<?> constructor = onlyFit(Arrays.asList(type.getConstructors()), args,
                "public constructor of " + type.getName());
        try {
            return type.cast(constructor.newInstance(args));
        } catch (InvocationTargetException e) {
            throw rethrow(e);
        } catch (InstantiationException | IllegalAccessException e) {
            // InstantiationException: the class is abstract.
            throw new IllegalArgumentException("Cannot create " + type.getName() + ": " + e, e);
        }
    }

    public static Object invoke(Object target, String methodName, Object... args) {
        Objects.requireNonNull(target, "target");
        Objects.requireNonNull(methodName, "methodName");
        Objects.requireNonNull(args, "args");
        // getMethods(): the public methods, inherited ones included.
        List<Method> candidates = Arrays.stream(target.getClass().getMethods())
                .filter(method -> method.getName().equals(methodName))
                .filter(method -> !method.isBridge())
                .toList();
        Method method = onlyFit(candidates, args,
                "public method " + target.getClass().getName() + "." + methodName + "(...)");
        try {
            return method.invoke(target, args);
        } catch (InvocationTargetException e) {
            throw rethrow(e);
        } catch (IllegalAccessException e) {
            // For example, a public method of a class that isn't public, in another package.
            throw new IllegalArgumentException("Cannot call " + method + ": " + e, e);
        }
    }

    public static String describeRecord(Record record) {
        Objects.requireNonNull(record, "record");
        // getRecordComponents() is guaranteed to follow the order of the record header.
        return Arrays.stream(record.getClass().getRecordComponents())
                .map(component -> component.getName() + "=" + String.valueOf(read(component, record)))
                .collect(Collectors.joining(", ", record.getClass().getSimpleName() + "[", "]"));
    }

    /** The one candidate whose parameters fit {@code args}; none or several is an IllegalArgumentException. */
    private static <E extends Executable> E onlyFit(List<E> candidates, Object[] args, String what) {
        List<E> fitting = candidates.stream()
                .filter(candidate -> fits(candidate.getParameterTypes(), args))
                .toList();
        if (fitting.size() == 1) {
            return fitting.getFirst();
        }
        String problem = fitting.isEmpty() ? "No " : "More than one ";
        throw new IllegalArgumentException(problem + what + " fits the arguments " + describeTypes(args));
    }

    private static boolean fits(Class<?>[] parameterTypes, Object[] args) {
        return parameterTypes.length == args.length
                && IntStream.range(0, args.length).allMatch(i -> fits(parameterTypes[i], args[i]));
    }

    private static boolean fits(Class<?> parameterType, Object arg) {
        // isInstance is false for primitive classes (int.class.isInstance(3) is false), so compare with the wrapper.
        return arg == null ? !parameterType.isPrimitive() : boxed(parameterType).isInstance(arg);
    }

    private static Object read(Field field, Object target) {
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            // Can't happen after setAccessible(true), but the compiler can't know that.
            throw new IllegalStateException(e);
        }
    }

    private static Object read(RecordComponent component, Record record) {
        // The accessor is a public method, so no setAccessible is needed for records in our own packages.
        Method accessor = component.getAccessor();
        try {
            return accessor.invoke(record);
        } catch (InvocationTargetException e) {
            throw rethrow(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot read " + accessor + ": " + e, e);
        }
    }

    /**
     * The exception to throw for a constructor or method that failed. Reflection wraps whatever the code threw in an
     * InvocationTargetException; the caller wants the original. Use it as {@code throw rethrow(e)}.
     */
    private static RuntimeException rethrow(InvocationTargetException e) {
        return new RuntimeException(e);
    }

    private static String describeTypes(Object[] args) {
        return Arrays.stream(args)
                .map(arg -> arg == null ? "null" : arg.getClass().getSimpleName())
                .collect(Collectors.joining(", ", "(", ")"));
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
