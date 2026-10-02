package practice.j03.ex02;

import java.util.Map;

/**
 * Thrown when an order line names a product that doesn't exist.
 *
 * <p>Code {@link ErrorCode#PRODUCT_NOT_FOUND}. Details: {@code sku}, so the message reads
 * "No product with SKU BOOK-404".
 */
public class ProductNotFoundException extends BusinessException {

    /** For a lookup that found nothing, for example an empty {@code Optional}. */
    public ProductNotFoundException(String sku) {
        // TODO: J03-02 — replace Map.of() with the details.
        // (Every constructor must start by calling a superclass constructor, and BusinessException has none without
        // arguments. That's why this stub calls super(...) instead of throwing UnsupportedOperationException.)
        super(ErrorCode.PRODUCT_NOT_FOUND, Map.of());
    }

    /** For translating a lower-level "not found" exception, which is kept as the cause. */
    public ProductNotFoundException(String sku, Throwable cause) {
        // TODO: J03-02 — pass the details and the cause to super.
        super(ErrorCode.PRODUCT_NOT_FOUND, Map.of());
    }
}
