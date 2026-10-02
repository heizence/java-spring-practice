package practice.j06.ex04;

/**
 * The client for the pricing service. Every call is a request over the network that takes about 200 ms in
 * production, so callers should only make it when they need the answer. Given code: it has no bugs.
 */
@FunctionalInterface
public interface PricingClient {

    /** The default discount, in percent, for a customer without a contract discount. */
    int defaultDiscountPercent(String customerId);
}
