package practice.j03.ex02;

import java.util.Map;

/**
 * Thrown when an order asks for more units than are in stock.
 *
 * <p>Code {@link ErrorCode#OUT_OF_STOCK}. Details: {@code sku}, {@code requested} and {@code available}, so the
 * message reads "Not enough stock for BOOK-1: requested 6, available 5".
 */
public class OutOfStockException extends BusinessException {

    public OutOfStockException(String sku, int requested, int available) {
        // TODO: J03-02 — replace Map.of() with the details.
        // (Every constructor must start by calling a superclass constructor, and BusinessException has none without
        // arguments. That's why this stub calls super(...) instead of throwing UnsupportedOperationException.)
        super(ErrorCode.OUT_OF_STOCK, Map.of());
    }
}
