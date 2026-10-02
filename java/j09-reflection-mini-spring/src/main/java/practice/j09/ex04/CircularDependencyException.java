package practice.j09.ex04;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Creating a bean needs that same bean again, directly or through other beans. Like Spring's
 * {@code BeanCurrentlyInCreationException}. Given code: you don't need to change it.
 */
public final class CircularDependencyException extends RuntimeException {

    private final List<Class<?>> path;

    /**
     * Creates the exception with a message such as {@code "Circular dependency: A -> B -> A"}.
     *
     * @param path the classes along the cycle, starting and ending with the same class, such as {@code [A, B, A]}
     * @throws IllegalArgumentException if the path is shorter than 2 or doesn't end where it starts
     */
    public CircularDependencyException(List<Class<?>> path) {
        super("Circular dependency: " + path.stream().map(Class::getSimpleName).collect(Collectors.joining(" -> ")));
        if (path.size() < 2 || path.getFirst() != path.getLast()) {
            throw new IllegalArgumentException(
                    "A cycle's path must start and end with the same class, such as [A, B, A], but was " + path);
        }
        this.path = List.copyOf(path);
    }

    /** The classes along the cycle, starting and ending with the same class. */
    public List<Class<?>> path() {
        return path;
    }
}
