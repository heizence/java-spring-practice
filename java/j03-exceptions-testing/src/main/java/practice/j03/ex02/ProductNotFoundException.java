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
        super(ErrorCode.PRODUCT_NOT_FOUND, Map.of("sku", sku));
    }

    /** For translating a lower-level "not found" exception, which is kept as the cause. */
    public ProductNotFoundException(String sku, Throwable cause) {
        super(ErrorCode.PRODUCT_NOT_FOUND, Map.of("sku", sku), cause);
    }
}
