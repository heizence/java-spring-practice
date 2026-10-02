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
        // The limits travel with the error, so a client can show "between 1 and 100" without knowing the rule.
        super(ErrorCode.INVALID_QUANTITY, Map.of("quantity", quantity, "min", min, "max", max));
    }
}
