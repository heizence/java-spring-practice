package practice.j06.ex04;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Customer lookups for the order emails and the checkout.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>{@link #contactEmail}: the customer's email address, or {@link #FALLBACK_EMAIL} when the customer has
 *       none.</li>
 *   <li>{@link #discountPercent}: the customer's contract discount if there is one. Otherwise, the default discount
 *       from the {@link PricingClient}. The pricing client makes a slow network call, so it is only called when
 *       there is no contract discount.</li>
 *   <li>Both methods throw {@link IllegalArgumentException} for an unknown customer id, with the id in the
 *       message.</li>
 * </ul>
 */
public final class CustomerService {

    /** Where order emails go for customers without an email address. */
    public static final String FALLBACK_EMAIL = "orders@shop.example";

    private final Map<String, Customer> customersById;
    private final Map<String, Integer> contractDiscounts;
    private final PricingClient pricing;

    /**
     * @param customers         the known customers
     * @param contractDiscounts customer id → discount in percent, for customers with a contract
     * @param pricing           the pricing service
     */
    public CustomerService(List<Customer> customers, Map<String, Integer> contractDiscounts, PricingClient pricing) {
        this.customersById = customers.stream()
                .collect(Collectors.toUnmodifiableMap(Customer::id, Function.identity()));
        this.contractDiscounts = Map.copyOf(contractDiscounts);
        this.pricing = Objects.requireNonNull(pricing, "pricing");
    }

    /** The customer with this id, or empty if there is none. */
    public Optional<Customer> find(String customerId) {
        return Optional.ofNullable(customersById.get(customerId));
    }

    /** The address to send order emails to. */
    public String contactEmail(String customerId) {
        return customer(customerId).email().orElseThrow();
    }

    /** The discount in percent for this customer. */
    public int discountPercent(String customerId) {
        Customer customer = customer(customerId);
        // orElse(pricing.defaultDiscountPercent(...)) would call the pricing service *before* orElse runs, even for
        // customers with a contract discount: Java evaluates every argument before calling a method. orElseGet takes
        // a Supplier, and calls it only when the Optional is empty.
        return Optional.ofNullable(contractDiscounts.get(customer.id()))
                .orElseGet(() -> pricing.defaultDiscountPercent(customer.id()));
    }

    private Customer customer(String customerId) {
        // get() would throw NoSuchElementException("No value present"), which tells the caller nothing.
        // orElseThrow with a supplier lets us choose the exception and the message.
        return find(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown customer: " + customerId));
    }
}
