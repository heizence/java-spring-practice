package practice.j08.ex06;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Loads the customer reviews of a product. Nice to have on the page, but not essential. */
@FunctionalInterface
public interface ReviewService {

    /** Starts loading and returns at once. The future completes with the review texts later, or fails. */
    CompletableFuture<List<String>> reviewsOf(String productId);
}
