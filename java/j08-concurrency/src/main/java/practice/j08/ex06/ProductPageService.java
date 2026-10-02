package practice.j08.ex06;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * Builds a product page from three services that answer asynchronously: price, stock and reviews.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #load} asks all three services right away, before any of them has answered, and returns at once with
 *       a future for the page. It never waits for a service (no {@code join()} or {@code get()}); it combines the
 *       services' futures instead, with {@code thenCombine} or {@code allOf}.</li>
 *   <li>The page completes when price, stock and reviews have all arrived, in any order.</li>
 *   <li>Reviews are optional. If the review service fails, or the reviews haven't arrived within {@code timeout},
 *       the page has an empty review list.</li>
 *   <li>Price and stock are required. When both have answered and one of them failed, the page fails with a
 *       {@link ProductPageException} whose cause is the exception of that service, not a wrapper such as
 *       {@link java.util.concurrent.CompletionException CompletionException}.</li>
 *   <li>When price and stock haven't both answered within {@code timeout}, the page fails with a
 *       {@link ProductPageException} whose cause is a {@link java.util.concurrent.TimeoutException TimeoutException}.
 *   </li>
 *   <li>Timeouts are measured from the moment {@code load} is called.</li>
 *   <li>{@code load(null)} throws {@link NullPointerException} at once. The constructor throws
 *       {@link NullPointerException} for a {@code null} argument and {@link IllegalArgumentException} for a timeout
 *       that isn't positive.</li>
 * </ul>
 */
public final class ProductPageService {

    /** Creates the service. {@code timeout} is how long a page may wait for the services. */
    public ProductPageService(PriceService prices, StockService stock, ReviewService reviews, Duration timeout) {
        // TODO: J08-06
        throw new UnsupportedOperationException("TODO: J08-06");
    }

    /** Starts loading the page of {@code productId} and returns at once. */
    public CompletableFuture<ProductPage> load(String productId) {
        // TODO: J08-06
        throw new UnsupportedOperationException("TODO: J08-06");
    }
}
