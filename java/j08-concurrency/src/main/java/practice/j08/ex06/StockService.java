package practice.j08.ex06;

import java.util.concurrent.CompletableFuture;

/** Looks up how many items of a product are in stock, for example in the warehouse system. */
@FunctionalInterface
public interface StockService {

    /** Starts the lookup and returns at once. The future completes with the number in stock later, or fails. */
    CompletableFuture<Integer> inStock(String productId);
}
