package practice.j09.ex04;

/**
 * A bean can't be created: its class has no usable constructor or {@code @Init} method, or one of them threw. Like
 * Spring's {@code BeanCreationException}. Given code: you don't need to change it.
 */
public final class BeanCreationException extends RuntimeException {

    private final Class<?> type;

    /**
     * Creates the exception with a message such as {@code "Cannot create Repository: it has no public constructor"}.
     */
    public BeanCreationException(Class<?> type, String reason) {
        this(type, reason, null);
    }

    /** The same, with the exception that caused the failure. */
    public BeanCreationException(Class<?> type, String reason, Throwable cause) {
        super("Cannot create " + type.getSimpleName() + ": " + reason, cause);
        this.type = type;
    }

    /** The class whose bean couldn't be created. */
    public Class<?> type() {
        return type;
    }
}
