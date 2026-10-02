package practice.j04.support;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Reads the generic signature of a method, for tests that check wildcards and bounds.
 *
 * <p>Type erasure removes the type arguments from the running code, but the compiler keeps every method's declared
 * signature in the class file, where reflection can read it. This class turns it into short, readable text. For
 * {@code static <T> T max(Collection<? extends T> items, Comparator<? super T> comparator)}:
 *
 * <ul>
 *   <li>{@code parameter(0)} is {@code "Collection<? extends T>"}</li>
 *   <li>{@code typeParameters()} is {@code "<T>"}</li>
 *   <li>{@code toString()} is {@code "<T> T max(Collection<? extends T>, Comparator<? super T>)"}</li>
 * </ul>
 *
 * <p>Class names are shown without their package, and a bound of {@code Object} isn't shown, because it's the
 * default.
 */
public final class GenericSignature {

    private final Method method;

    private GenericSignature(Method method) {
        this.method = method;
    }

    /** Reads the only method called {@code name} in {@code owner}. Fails if there is none, or more than one. */
    public static GenericSignature of(Class<?> owner, String name) {
        List<Method> methods = Arrays.stream(owner.getDeclaredMethods())
                .filter(method -> method.getName().equals(name) && !method.isSynthetic())
                .toList();
        if (methods.size() != 1) {
            throw new AssertionError(("Expected exactly one method named %s in %s, but found %d. "
                    + "Keep the method names, and don't add overloads.")
                    .formatted(name, owner.getSimpleName(), methods.size()));
        }
        return new GenericSignature(methods.getFirst());
    }

    /** The declared type of the parameter at {@code index}, counting from 0. For example {@code "List<? super T>"}. */
    public String parameter(int index) {
        return render(method.getGenericParameterTypes()[index]);
    }

    /** The type parameters with their bounds, for example {@code "<T extends Comparable<? super T>>"}, or "". */
    public String typeParameters() {
        TypeVariable<Method>[] variables = method.getTypeParameters();
        if (variables.length == 0) {
            return "";
        }
        return Arrays.stream(variables)
                .map(GenericSignature::declaration)
                .collect(Collectors.joining(", ", "<", ">"));
    }

    /** The whole signature, for example {@code "<T> T max(Collection<? extends T>, Comparator<? super T>)"}. */
    @Override
    public String toString() {
        String typeParameters = typeParameters();
        String parameters = Arrays.stream(method.getGenericParameterTypes())
                .map(GenericSignature::render)
                .collect(Collectors.joining(", ", "(", ")"));
        return (typeParameters.isEmpty() ? "" : typeParameters + " ")
                + render(method.getGenericReturnType()) + " " + method.getName() + parameters;
    }

    private static String declaration(TypeVariable<?> variable) {
        String bounds = Arrays.stream(variable.getBounds())
                .filter(bound -> bound != Object.class)
                .map(GenericSignature::render)
                .collect(Collectors.joining(" & "));
        return bounds.isEmpty() ? variable.getName() : variable.getName() + " extends " + bounds;
    }

    private static String render(Type type) {
        return switch (type) {
            case Class<?> c when c.isArray() -> render(c.getComponentType()) + "[]";
            case Class<?> c -> c.getSimpleName();
            case ParameterizedType p -> render(p.getRawType()) + Arrays.stream(p.getActualTypeArguments())
                    .map(GenericSignature::render)
                    .collect(Collectors.joining(", ", "<", ">"));
            case WildcardType w when w.getLowerBounds().length > 0 -> "? super " + render(w.getLowerBounds()[0]);
            case WildcardType w when w.getUpperBounds()[0] == Object.class -> "?";
            case WildcardType w -> "? extends " + render(w.getUpperBounds()[0]);
            case TypeVariable<?> v -> v.getName();
            case GenericArrayType g -> render(g.getGenericComponentType()) + "[]";
            default -> type.getTypeName();
        };
    }
}
