package practice.j08.ex06;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.j08.support.Blocking;

/**
 * The services are fakes: each one returns a future that the test completes, fails, or never completes. So the
 * tests decide exactly when each service answers, and no test depends on timing, except the ones about timeouts.
 */
@DisplayName("J08-06 · CompletableFuture")
class ProductPageServiceTest {

    private static final String PRODUCT = "book-42";
    private static final BigDecimal PRICE = new BigDecimal("19.99");
    /** Long enough that tests about other things never reach it. */
    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(1);
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(100);

    // The answers of the three services.
    private final CompletableFuture<BigDecimal> price = new CompletableFuture<>();
    private final CompletableFuture<Integer> stock = new CompletableFuture<>();
    private final CompletableFuture<List<String>> reviews = new CompletableFuture<>();
    /** Which services were asked, for example "price book-42". */
    private final List<String> calls = new CopyOnWriteArrayList<>();

    private ProductPageService serviceWithTimeout(Duration timeout) {
        return new ProductPageService(
                id -> {
                    calls.add("price " + id);
                    return price;
                },
                id -> {
                    calls.add("stock " + id);
                    return stock;
                },
                id -> {
                    calls.add("reviews " + id);
                    return reviews;
                },
                timeout);
    }

    /** Waits until the page fails, and returns the {@link ProductPageException} it failed with. */
    private static ProductPageException failureOf(CompletableFuture<ProductPage> page) {
        assertThat(page).failsWithin(Blocking.PATIENCE)
                .withThrowableOfType(ExecutionException.class)
                .withCauseInstanceOf(ProductPageException.class);
        return (ProductPageException) page.exceptionNow();
    }

    @AfterEach
    void answerEverything() {
        // Code that (wrongly) waits for one of these futures would otherwise wait forever.
        price.complete(PRICE);
        stock.complete(0);
        reviews.complete(List.of());
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("builds the page from price, stock and reviews")
        void buildsPage() {
            CompletableFuture<ProductPage> page = serviceWithTimeout(LONG_TIMEOUT).load(PRODUCT);

            price.complete(PRICE);
            stock.complete(7);
            reviews.complete(List.of("Great book", "Too long"));

            assertThat(page).succeedsWithin(Blocking.PATIENCE)
                    .isEqualTo(new ProductPage(PRODUCT, PRICE, 7, List.of("Great book", "Too long")));
        }

        @Test
        @DisplayName("asks all three services at once, before any of them has answered")
        void asksAllServicesAtOnce() {
            serviceWithTimeout(LONG_TIMEOUT).load(PRODUCT);

            assertThat(calls).containsExactlyInAnyOrder("price book-42", "stock book-42", "reviews book-42");
        }

        @Test
        @DisplayName("load() returns at once, without waiting for the services")
        void returnsAtOnce() {
            ProductPageService service = serviceWithTimeout(LONG_TIMEOUT);

            CompletableFuture<ProductPage> page = assertTimeoutPreemptively(Duration.ofMillis(500),
                    () -> service.load(PRODUCT), "load() waited for the services");

            assertThat(page).isNotDone();
        }

        @Test
        @DisplayName("the page completes when the last service answers, in any order")
        void completesWhenLastServiceAnswers() {
            CompletableFuture<ProductPage> page = serviceWithTimeout(LONG_TIMEOUT).load(PRODUCT);

            reviews.complete(List.of("Great book"));
            stock.complete(3);
            assertThat(page).as("before the price has arrived").isNotDone();
            price.complete(PRICE);

            assertThat(page).succeedsWithin(Blocking.PATIENCE)
                    .isEqualTo(new ProductPage(PRODUCT, PRICE, 3, List.of("Great book")));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("shows no reviews when the review service fails")
        void noReviewsWhenReviewServiceFails() {
            CompletableFuture<ProductPage> page = serviceWithTimeout(LONG_TIMEOUT).load(PRODUCT);

            price.complete(PRICE);
            stock.complete(7);
            reviews.completeExceptionally(new IllegalStateException("review service down"));

            assertThat(page).succeedsWithin(Blocking.PATIENCE).isEqualTo(new ProductPage(PRODUCT, PRICE, 7, List.of()));
        }

        @Test
        @DisplayName("shows no reviews when they don't arrive within the timeout")
        void noReviewsWhenReviewsAreTooSlow() {
            CompletableFuture<ProductPage> page = serviceWithTimeout(SHORT_TIMEOUT).load(PRODUCT);

            price.complete(PRICE);
            stock.complete(7);
            // The reviews never arrive.

            assertThat(page).succeedsWithin(Blocking.PATIENCE).isEqualTo(new ProductPage(PRODUCT, PRICE, 7, List.of()));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("fails with ProductPageException, caused by the price service's own exception")
        void failsWhenPriceServiceFails() {
            var failure = new IllegalStateException("price service down");
            CompletableFuture<ProductPage> page = serviceWithTimeout(LONG_TIMEOUT).load(PRODUCT);

            stock.complete(7);
            reviews.complete(List.of());
            price.completeExceptionally(failure);

            ProductPageException error = failureOf(page);
            assertThat(error.getCause()).isSameAs(failure);
            assertThat(error.productId()).isEqualTo(PRODUCT);
        }

        @Test
        @DisplayName("fails with ProductPageException, caused by the stock service's own exception")
        void failsWhenStockServiceFails() {
            var failure = new IllegalStateException("warehouse offline");
            CompletableFuture<ProductPage> page = serviceWithTimeout(LONG_TIMEOUT).load(PRODUCT);

            price.complete(PRICE);
            reviews.complete(List.of());
            stock.completeExceptionally(failure);

            assertThat(failureOf(page).getCause()).isSameAs(failure);
        }

        @Test
        @DisplayName("fails with a TimeoutException as cause when the price doesn't arrive within the timeout")
        void failsWhenPriceIsTooSlow() {
            CompletableFuture<ProductPage> page = serviceWithTimeout(SHORT_TIMEOUT).load(PRODUCT);

            stock.complete(7);
            reviews.complete(List.of());
            // The price never arrives.

            assertThat(failureOf(page).getCause()).isInstanceOf(TimeoutException.class);
        }

        @Test
        @DisplayName("fails with a TimeoutException as cause when the stock doesn't arrive within the timeout")
        void failsWhenStockIsTooSlow() {
            CompletableFuture<ProductPage> page = serviceWithTimeout(SHORT_TIMEOUT).load(PRODUCT);

            price.complete(PRICE);
            reviews.complete(List.of());
            // The stock never arrives.

            assertThat(failureOf(page).getCause()).isInstanceOf(TimeoutException.class);
        }

        @Test
        @DisplayName("load() rejects a null product id at once")
        void rejectsNullProductId() {
            ProductPageService service = serviceWithTimeout(LONG_TIMEOUT);

            assertThatThrownBy(() -> service.load(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("the constructor rejects null services and a timeout that isn't positive")
        void constructorRejectsBadArguments() {
            PriceService prices = id -> price;
            StockService stocks = id -> stock;
            ReviewService reviewService = id -> reviews;

            assertThatThrownBy(() -> new ProductPageService(null, stocks, reviewService, LONG_TIMEOUT))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ProductPageService(prices, null, reviewService, LONG_TIMEOUT))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ProductPageService(prices, stocks, null, LONG_TIMEOUT))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ProductPageService(prices, stocks, reviewService, null))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new ProductPageService(prices, stocks, reviewService, Duration.ZERO))
                    .isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> new ProductPageService(prices, stocks, reviewService, Duration.ofSeconds(-1)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
