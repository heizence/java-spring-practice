package practice.j03.ex02;

import java.util.Map;

/**
 * The body that a REST API sends back when a business rule is broken, for example:
 *
 * <pre>
 * { "status": 409,
 *   "code": "ORDER-001",
 *   "message": "Not enough stock for BOOK-1: requested 6, available 5",
 *   "details": { "sku": "BOOK-1", "requested": 6, "available": 5 } }
 * </pre>
 *
 * <p>Spring has a standard type for this, {@code ProblemDetail} (RFC 9457), which you'll use in S04. Its
 * {@code status} and {@code detail} fields match {@code status} and {@code message} here, and extra fields such
 * as {@code code} go into its {@code properties}.
 *
 * @param status  the HTTP status code, for example 409
 * @param code    the stable error code, for example {@code "ORDER-001"}
 * @param message the human-readable message
 * @param details the values that describe the error
 */
public record ErrorResponse(int status, String code, String message, Map<String, Object> details) {

    /**
     * Builds the response for {@code exception}. The status and the code come from its {@link ErrorCode}, the
     * message is its {@link BusinessException#getMessage() message}, and the details are its
     * {@link BusinessException#details() details}.
     */
    public static ErrorResponse from(BusinessException exception) {
        // TODO: J03-02
        throw new UnsupportedOperationException("TODO: J03-02");
    }
}
