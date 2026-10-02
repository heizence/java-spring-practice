package practice.j06.ex04;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J06-04 · Optional and stream pitfalls")
class PitfallsTest {

    private static final Order A1 = new Order("A-1", "mina", true, 4_000);
    private static final Order A2 = new Order("A-2", "jun", false, 1_500);
    private static final Order A3 = new Order("A-3", "mina", true, 200);
    private static final Order A4 = new Order("A-4", "ari", false, 900);
    private static final Order A5 = new Order("A-5", "jun", false, 300);
    private static final List<Order> ORDERS = List.of(A1, A2, A3, A4, A5);

    private final AtomicInteger pricingCalls = new AtomicInteger();
    private final PricingClient pricing = customerId -> {
        pricingCalls.incrementAndGet();
        return 5;
    };

    private final Customer mina = new Customer("c-1", "Mina", "mina@example.com");
    private final Customer jun = new Customer("c-2", "Jun"); // signed up by phone: no email
    private final CustomerService service = new CustomerService(List.of(mina, jun), Map.of("c-1", 15), pricing);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("Customer.email() returns the address of a customer who has one")
        void emailPresent() {
            assertThat(mina.email()).contains("mina@example.com");
        }

        @Test
        @DisplayName("contactEmail() returns the customer's email address")
        void contactEmail() {
            assertThat(service.contactEmail("c-1")).isEqualTo("mina@example.com");
        }

        @Test
        @DisplayName("discountPercent() returns the contract discount when the customer has one")
        void contractDiscount() {
            assertThat(service.discountPercent("c-1")).isEqualTo(15);
        }

        @Test
        @DisplayName("discountPercent() asks the pricing service when the customer has no contract discount")
        void defaultDiscount() {
            assertThat(service.discountPercent("c-2")).isEqualTo(5);
        }

        @Test
        @DisplayName("paidSummary() counts and adds up the paid orders")
        void paidSummary() {
            assertThat(OrderStats.paidSummary(ORDERS)).isEqualTo("2 paid orders, 4200 cents");
        }

        @Test
        @DisplayName("customersInOrderOfFirstOrder() lists each customer once")
        void customersOnce() {
            assertThat(OrderStats.customersInOrderOfFirstOrder(ORDERS))
                    .containsExactlyInAnyOrder("mina", "jun", "ari");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a customer created without an email has an empty email(), not null")
        void noEmailIsEmpty() {
            Optional<String> email = jun.email();

            assertThat(email).isNotNull().isEmpty();
        }

        @Test
        @DisplayName("a null email passed to the constructor means \"no email\"")
        void nullEmailMeansNoEmail() {
            var customer = new Customer("c-3", "Ari", null);

            assertThat(customer.email()).isEmpty();
        }

        @Test
        @DisplayName("Customer stores the email in a plain field: Optional is meant for return values only")
        void noOptionalFields() {
            assertThat(Customer.class.getDeclaredFields())
                    .extracting(Field::getType)
                    .doesNotContain(Optional.class);
        }

        @Test
        @DisplayName("contactEmail() falls back to FALLBACK_EMAIL for a customer without an email")
        void contactEmailFallback() {
            assertThat(service.contactEmail("c-2")).isEqualTo(CustomerService.FALLBACK_EMAIL);
        }

        @Test
        @DisplayName("discountPercent() doesn't call the pricing service when there is a contract discount")
        void contractDiscountSkipsPricing() {
            service.discountPercent("c-1");

            assertThat(pricingCalls).hasValue(0);
        }

        @Test
        @DisplayName("discountPercent() calls the pricing service once when there is no contract discount")
        void defaultDiscountCallsPricingOnce() {
            service.discountPercent("c-2");

            assertThat(pricingCalls).hasValue(1);
        }

        @Test
        @DisplayName("paidSummary() works when no order is paid")
        void paidSummaryWithoutPaidOrders() {
            assertThat(OrderStats.paidSummary(List.of(A2, A4))).isEqualTo("0 paid orders, 0 cents");
        }

        @Test
        @DisplayName("paidSummary() works for an empty batch")
        void paidSummaryEmpty() {
            assertThat(OrderStats.paidSummary(List.of())).isEqualTo("0 paid orders, 0 cents");
        }

        @Test
        @DisplayName("customersInOrderOfFirstOrder() keeps the order of each customer's first order")
        void customersKeepFirstOrder() {
            assertThat(OrderStats.customersInOrderOfFirstOrder(ORDERS)).containsExactly("mina", "jun", "ari");
        }

        @Test
        @DisplayName("empty batches give empty results")
        void emptyBatches() {
            assertThat(OrderStats.customersInOrderOfFirstOrder(List.of())).isEmpty();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("contactEmail() rejects an unknown customer id with IllegalArgumentException that names the id")
        void contactEmailUnknownCustomer() {
            assertThatThrownBy(() -> service.contactEmail("c-404"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("c-404");
        }

        @Test
        @DisplayName("discountPercent() rejects an unknown customer id with IllegalArgumentException that names the id")
        void discountUnknownCustomer() {
            assertThatThrownBy(() -> service.discountPercent("c-404"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("c-404");
        }

        @Test
        @DisplayName("Customer rejects a null id or name")
        void customerRejectsNulls() {
            assertThatThrownBy(() -> new Customer(null, "Mina")).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Customer("c-1", null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Customer("c-1", null, "mina@example.com"))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("every OrderStats method rejects a null list")
        void orderStatsRejectNull() {
            assertThatThrownBy(() -> OrderStats.paidSummary(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> OrderStats.customersInOrderOfFirstOrder(null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
