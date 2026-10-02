package practice.j03.ex02;

import java.util.Map;

/**
 * Thrown when an order line asks for a quantity outside the allowed range.
 *
 * <p>Code {@link ErrorCode#INVALID_QUANTITY}. Details: {@code quantity}, {@code min} and {@code max}, so the message
 * reads "Quantity must be between 1 and 100, but was 0".
 */
public class InvalidQuantityException extends BusinessException {

    public InvalidQuantityException(int quantity, int min, int max) {
        // TODO: J03-02 — replace Map.of() with the details.
        // (Every constructor must start by calling a superclass constructor, and BusinessException has none without
        // arguments. That's why this stub calls super(...) instead of throwing UnsupportedOperationException.)
        super(ErrorCode.INVALID_QUANTITY, Map.of());
    }
}
