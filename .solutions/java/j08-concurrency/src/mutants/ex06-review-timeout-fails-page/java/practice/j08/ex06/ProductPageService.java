package practice.j08.ex06;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;

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

    private final PriceService prices;
    private final StockService stock;
    private final ReviewService reviews;
    private final Duration timeout;

    /** Creates the service. {@code timeout} is how long a page may wait for the services. */
    public ProductPageService(PriceService prices, StockService stock, ReviewService reviews, Duration timeout) {
        this.prices = Objects.requireNonNull(prices, "prices");
        this.stock = Objects.requireNonNull(stock, "stock");
        this.reviews = Objects.requireNonNull(reviews, "reviews");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative() || timeout.isZero()) {
            throw new IllegalArgumentException("timeout must be positive, but was " + timeout);
        }
        this.timeout = timeout;
    }

    /** Starts loading the page of {@code productId} and returns at once. */
    public CompletableFuture<ProductPage> load(String productId) {
        Objects.requireNonNull(productId, "productId");
        long timeoutNanos = timeout.toNanos();

        // Ask all three services first. Each call returns at once, so the three lookups run at the same time.
        CompletableFuture<BigDecimal> price = prices.priceOf(productId);
        CompletableFuture<Integer> inStock = stock.inStock(productId);
        CompletableFuture<List<String>> reviewTexts = reviews.reviewsOf(productId)
                // Reviews are optional: a failure becomes an empty list...
                .exceptionally(failure -> List.of())
                // ...and so do reviews that take too long. exceptionally() returned a new future, so this timeout
                // doesn't change the future that the review service gave us (other code may share that one).
                .orTimeout(timeoutNanos, TimeUnit.NANOSECONDS);

        // Price and stock are required. allOf completes when both have completed, and fails if either one failed.
        // orTimeout makes it fail with a TimeoutException if that takes too long.
        CompletableFuture<Void> priceAndStock = CompletableFuture.allOf(price, inStock)
                .orTimeout(timeoutNanos, TimeUnit.NANOSECONDS);

        return priceAndStock
                // join() doesn't wait here: this function only runs after allOf has completed, so both are done.
                .thenCombine(reviewTexts,
                        (done, texts) -> new ProductPage(productId, price.join(), inStock.join(), texts))
                .exceptionally(failure -> {
                    throw new ProductPageException(productId, unwrap(failure));
                });
    }

    /**
     * A failure that travels along a chain of stages arrives wrapped in a {@link CompletionException}. Callers want
     * the original exception, so we take it out of the wrapper.
     */
    private static Throwable unwrap(Throwable failure) {
        return failure instanceof CompletionException && failure.getCause() != null ? failure.getCause() : failure;
    }
}
