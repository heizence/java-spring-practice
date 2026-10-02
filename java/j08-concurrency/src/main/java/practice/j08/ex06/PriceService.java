package practice.j08.ex06;

import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;

/** Looks up the current price of a product, for example by calling another service over HTTP. */
@FunctionalInterface
public interface PriceService {

    /** Starts the lookup and returns at once. The future completes with the price later, or fails. */
    CompletableFuture<BigDecimal> priceOf(String productId);
}
