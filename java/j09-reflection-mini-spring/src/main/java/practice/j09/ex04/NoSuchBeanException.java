package practice.j09.ex04;

import java.util.Objects;

/**
 * No component fits a requested type. Like Spring's {@code NoSuchBeanDefinitionException}. Given code: you don't
 * need to change it.
 */
public final class NoSuchBeanException extends RuntimeException {

    private final Class<?> type;

    /** Creates the exception with a message such as {@code "No bean of type PaymentGateway"}. */
    public NoSuchBeanException(Class<?> type) {
        super("No bean of type " + type.getSimpleName());
        this.type = Objects.requireNonNull(type, "type");
    }

    /** The type that was requested. */
    public Class<?> type() {
        return type;
    }
}
