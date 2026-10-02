package practice.j09.ex04;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Several components fit a requested type, and {@link Primary} doesn't decide between them. Like Spring's
 * {@code NoUniqueBeanDefinitionException}. Given code: you don't need to change it.
 */
public final class NoUniqueBeanException extends RuntimeException {

    private final Class<?> type;
    private final List<Class<?>> candidates;

    /**
     * Creates the exception with a message such as
     * {@code "Expected one bean of type Notifier but found 2: EmailNotifier, SmsNotifier"}. The candidates are
     * listed by simple name, in alphabetical order.
     */
    public NoUniqueBeanException(Class<?> type, Collection<Class<?>> candidates) {
        super("Expected one bean of type " + type.getSimpleName() + " but found " + candidates.size() + ": "
                + sorted(candidates).stream().map(Class::getSimpleName).collect(Collectors.joining(", ")));
        this.type = type;
        this.candidates = sorted(candidates);
    }

    /** The type that was requested. */
    public Class<?> type() {
        return type;
    }

    /** The components that fit, sorted by simple name. */
    public List<Class<?>> candidates() {
        return candidates;
    }

    private static List<Class<?>> sorted(Collection<Class<?>> candidates) {
        return candidates.stream().sorted(Comparator.comparing(Class::getSimpleName)).toList();
    }
}
