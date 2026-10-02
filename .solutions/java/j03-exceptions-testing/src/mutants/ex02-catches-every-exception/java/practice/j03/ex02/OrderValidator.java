package practice.j03.ex02;

import java.util.Objects;

/**
 * Checks an order line before the order is placed, and reports every problem as a {@link BusinessException}.
 *
 * <h2>Rules</h2>
 * The checks run in this order, and the first one that fails throws:
 * <ol>
 *   <li>{@code sku} must not be {@code null}, otherwise {@link NullPointerException}. A {@code null} SKU is a bug in
 *       the calling code, not a business error.</li>
 *   <li>{@code quantity} must be between {@value #MIN_QUANTITY} and {@value #MAX_QUANTITY}, inclusive, otherwise
 *       {@link InvalidQuantityException}. This check runs before the stock lookup because it needs none.</li>
 *   <li>The product must exist. When {@link Inventory#stockOf} throws {@link java.util.NoSuchElementException},
 *       throw {@link ProductNotFoundException} with that exception as its cause. Any other exception from the
 *       inventory (for example, its database is down) is <em>not</em> a "not found", so it propagates
 *       unchanged.</li>
 *   <li>The stock must cover the quantity, otherwise {@link OutOfStockException}. Ordering exactly the units that
 *       are left is fine.</li>
 * </ol>
 */
public class OrderValidator {

    public static final int MIN_QUANTITY = 1;
    public static final int MAX_QUANTITY = 100;

    private final Inventory inventory;

    /** @throws NullPointerException if {@code inventory} is {@code null} */
    public OrderValidator(Inventory inventory) {
        this.inventory = Objects.requireNonNull(inventory, "inventory");
    }

    /** Returns normally if the order line can be placed, and throws a {@link BusinessException} if not. */
    public void validate(String sku, int quantity) {
        Objects.requireNonNull(sku, "sku");
        if (quantity < MIN_QUANTITY || quantity > MAX_QUANTITY) {
            throw new InvalidQuantityException(quantity, MIN_QUANTITY, MAX_QUANTITY);
        }
        int stock = stockOf(sku);
        if (stock < quantity) {
            throw new OutOfStockException(sku, quantity, stock);
        }
    }

    private int stockOf(String sku) {
        try {
            return inventory.stockOf(sku);
        } catch (RuntimeException e) {
            // Exception translation: the lower layer's "no such row" becomes our business error, and the original
            // stays attached as the cause for whoever reads the stack trace.
            throw new ProductNotFoundException(sku, e);
        }
    }
}
