package practice.j03.ex02;

import java.util.Map;

/**
 * Base class for errors that break a business rule, such as ordering more than is in stock.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>It's unchecked. Business errors travel up to one central handler (in Spring, a
 *       {@code @RestControllerAdvice}) that turns them into an HTTP response, so the code in between doesn't have
 *       to declare or catch them.</li>
 *   <li>It's abstract. Code throws a specific subclass such as {@link OutOfStockException}, never a plain
 *       {@code BusinessException}.</li>
 *   <li>{@link #getMessage()} is the code's template filled in with the details ({@link ErrorCode#format}).</li>
 *   <li>{@link #details()} returns an unmodifiable copy of the details given to the constructor: changing that map
 *       later doesn't change the exception.</li>
 *   <li>A cause given to the constructor is kept, and {@link #getCause()} returns it.</li>
 *   <li>A {@code null} code or details map, or a {@code null} key or value inside the map, throws
 *       {@link NullPointerException}.</li>
 * </ul>
 */
public abstract class BusinessException extends RuntimeException {

    /** Creates an exception without a cause. */
    protected BusinessException(ErrorCode code, Map<String, Object> details) {
        // TODO: J03-02
        throw new UnsupportedOperationException("TODO: J03-02");
    }

    /** Creates an exception that another exception caused, for example a lower-level one that it translates. */
    protected BusinessException(ErrorCode code, Map<String, Object> details, Throwable cause) {
        // TODO: J03-02
        throw new UnsupportedOperationException("TODO: J03-02");
    }

    /** The error code, for example {@link ErrorCode#OUT_OF_STOCK}. */
    public ErrorCode code() {
        // TODO: J03-02
        throw new UnsupportedOperationException("TODO: J03-02");
    }

    /** The values that describe this error, for example {@code "sku" → "BOOK-1"}. */
    public Map<String, Object> details() {
        // TODO: J03-02
        throw new UnsupportedOperationException("TODO: J03-02");
    }
}
