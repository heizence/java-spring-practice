package practice.j08.ex06;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Everything the product page shows. It is immutable, so it can be passed between threads safely.
 *
 * @param productId the product
 * @param price     the current price
 * @param stock     how many items are in stock
 * @param reviews   the review texts; empty when the reviews were not available
 */
public record ProductPage(String productId, BigDecimal price, int stock, List<String> reviews) {

    public ProductPage {
        Objects.requireNonNull(productId, "productId");
        Objects.requireNonNull(price, "price");
        reviews = List.copyOf(reviews);
    }
}
