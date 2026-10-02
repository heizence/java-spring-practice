package practice.j06.ex03;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static practice.j06.ex03.Status.CANCELLED;
import static practice.j06.ex03.Status.NEW;
import static practice.j06.ex03.Status.PAID;
import static practice.j06.ex03.Status.SHIPPED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("J06-03 · Collectors")
class SalesReportTest {

    private static Order order(String id, String customer, Status status, OrderLine... lines) {
        return new Order(id, customer, status, List.of(lines));
    }

    private static OrderLine line(String product, String category, int quantity, long unitPriceCents) {
        return new OrderLine(product, category, quantity, unitPriceCents);
    }

    // Totals: O-1 4,100 · O-2 4,000 · O-3 5,900 · O-4 25,000 (cancelled) · O-5 4,200 · O-6 2,000
    private static final Order O1 = order("O-1", "mina", PAID,
            line("Clean Code", "books", 1, 3_500), line("Pen", "office", 3, 200));
    private static final Order O2 = order("O-2", "jun", SHIPPED, line("Lamp", "home", 1, 4_000));
    private static final Order O3 = order("O-3", "mina", NEW,
            line("Mug", "home", 2, 1_200), line("Clean Code", "books", 1, 3_500));
    private static final Order O4 = order("O-4", "ari", CANCELLED, line("Desk", "home", 1, 25_000));
    private static final Order O5 = order("O-5", "jun", PAID, line("Refactoring", "books", 1, 4_200));
    private static final Order O6 = order("O-6", "sora", PAID, line("Pen", "office", 10, 200));

    private static final List<Order> ORDERS = List.of(O1, O2, O3, O4, O5, O6);

    private final SalesReport report = new SalesReport(ORDERS);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("step 1: revenueByCategory() sums quantity × unit price per category, alphabetically")
        void revenueByCategory() {
            assertThat(report.revenueByCategory())
                    .containsExactly(entry("books", 11_200L), entry("home", 6_400L), entry("office", 2_600L));
        }

        @Test
        @DisplayName("step 2: orderCountByStatus() counts the orders of each status, in enum order")
        void orderCountByStatus() {
            assertThat(report.orderCountByStatus())
                    .containsExactly(entry(NEW, 1L), entry(PAID, 3L), entry(SHIPPED, 1L), entry(CANCELLED, 1L));
        }

        @Test
        @DisplayName("step 3: partitionByTotal() splits the order ids into big (true) and small (false)")
        void partitionByTotal() {
            Map<Boolean, List<String>> partition = report.partitionByTotal(4_100);

            assertThat(partition.get(true)).containsExactly("O-1", "O-3", "O-5");
            assertThat(partition.get(false)).containsExactly("O-2", "O-6");
        }

        @Test
        @DisplayName("step 4: productsByCustomer() lists each customer's products, both alphabetically")
        void productsByCustomer() {
            var products = report.productsByCustomer();

            assertThat(products).containsOnlyKeys("jun", "mina", "sora");
            assertThat(products.keySet()).containsExactly("jun", "mina", "sora");
            assertThat(products.get("jun")).containsExactly("Lamp", "Refactoring");
            assertThat(products.get("mina")).containsExactly("Clean Code", "Mug", "Pen");
            assertThat(products.get("sora")).containsExactly("Pen");
        }

        @Test
        @DisplayName("step 5: totalByCustomer() adds up each customer's orders, in the order of their first order")
        void totalByCustomer() {
            assertThat(report.totalByCustomer())
                    .containsExactly(entry("mina", 10_000L), entry("jun", 8_200L), entry("sora", 2_000L));
        }

        @Test
        @DisplayName("step 6: revenueCsv() writes a header line, then one line per category")
        void revenueCsv() {
            assertThat(report.revenueCsv())
                    .isEqualTo("category,revenue_cents\nbooks,11200\nhome,6400\noffice,2600");
        }

        @Test
        @DisplayName("step 7: topCustomerByCategory() names the biggest spender of each category")
        void topCustomerByCategory() {
            assertThat(report.topCustomerByCategory())
                    .containsExactly(entry("books", "mina"), entry("home", "jun"), entry("office", "sora"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        private final SalesReport empty = new SalesReport(List.of());

        @Test
        @DisplayName("an empty report gives empty maps")
        void emptyReportGivesEmptyMaps() {
            assertThat(empty.revenueByCategory()).isEmpty();
            assertThat(empty.orderCountByStatus()).isEmpty();
            assertThat(empty.productsByCustomer()).isEmpty();
            assertThat(empty.totalByCustomer()).isEmpty();
            assertThat(empty.topCustomerByCategory()).isEmpty();
        }

        @Test
        @DisplayName("revenueByCategory() leaves out cancelled orders")
        void revenueLeavesOutCancelled() {
            var cancelledOnly = new SalesReport(List.of(O4));

            assertThat(cancelledOnly.revenueByCategory()).isEmpty();
        }

        @Test
        @DisplayName("orderCountByStatus() returns an EnumMap")
        void orderCountIsEnumMap() {
            assertThat(report.orderCountByStatus()).isInstanceOf(EnumMap.class);
        }

        @Test
        @DisplayName("orderCountByStatus() leaves out statuses without orders")
        void orderCountLeavesOutMissingStatuses() {
            var paidOnly = new SalesReport(List.of(O1, O5));

            assertThat(paidOnly.orderCountByStatus()).containsExactly(entry(PAID, 2L));
        }

        @Test
        @DisplayName("partitionByTotal() puts an order whose total equals the threshold under true")
        void partitionThresholdIsInclusive() {
            assertThat(report.partitionByTotal(4_100).get(true)).contains("O-1");
        }

        @Test
        @DisplayName("partitionByTotal() always has both keys, even when one side is empty")
        void partitionHasBothKeys() {
            Map<Boolean, List<String>> everythingBig = report.partitionByTotal(1);

            assertThat(everythingBig).containsOnlyKeys(true, false);
            assertThat(everythingBig.get(false)).isEmpty();
            assertThat(empty.partitionByTotal(1)).containsOnlyKeys(true, false);
        }

        @Test
        @DisplayName("partitionByTotal() leaves out cancelled orders")
        void partitionLeavesOutCancelled() {
            Map<Boolean, List<String>> partition = report.partitionByTotal(1);

            assertThat(partition.get(true)).doesNotContain("O-4");
        }

        @Test
        @DisplayName("productsByCustomer() lists a product bought twice only once")
        void productsAreDistinct() {
            assertThat(report.productsByCustomer().get("mina")).containsOnlyOnce("Clean Code");
        }

        @Test
        @DisplayName("customers whose only orders were cancelled don't appear")
        void cancelledOnlyCustomersLeftOut() {
            assertThat(report.productsByCustomer()).doesNotContainKey("ari");
            assertThat(report.totalByCustomer()).doesNotContainKey("ari");
        }

        @Test
        @DisplayName("revenueCsv() of a report without orders that count is the header line alone")
        void csvOfEmptyReport() {
            assertThat(empty.revenueCsv()).isEqualTo("category,revenue_cents");
        }

        @Test
        @DisplayName("topCustomerByCategory() adds up a customer's purchases instead of taking the biggest single one")
        void topCustomerUsesSums() {
            // In "books", mina bought 3,500 twice (7,000 in all); jun bought once for 4,200.
            assertThat(report.topCustomerByCategory()).containsEntry("books", "mina");
        }

        @Test
        @DisplayName("topCustomerByCategory() breaks a tie with the name that comes first alphabetically")
        void topCustomerTieGoesToFirstName() {
            var tie = new SalesReport(List.of(
                    order("T-1", "yuna", PAID, line("Tea", "food", 1, 500)),
                    order("T-2", "bora", PAID, line("Jam", "food", 1, 500))));

            assertThat(tie.topCustomerByCategory()).containsExactly(entry("food", "bora"));
        }

        @Test
        @DisplayName("the report doesn't change when the caller changes the original list")
        void reportKeepsItsOwnCopy() {
            var orders = new ArrayList<>(List.of(O1, O2));
            var copy = new SalesReport(orders);

            orders.clear();

            assertThat(copy.totalByCustomer()).containsExactly(entry("mina", 4_100L), entry("jun", 4_000L));
        }

        @Test
        @DisplayName("changing a returned map doesn't change the next result")
        void resultsAreFresh() {
            report.revenueByCategory().put("garden", 1L);

            assertThat(report.revenueByCategory()).doesNotContainKey("garden");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a null list")
        void rejectsNullList() {
            assertThatThrownBy(() -> new SalesReport(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a list that contains null")
        void rejectsNullOrder() {
            var orders = Arrays.asList(O1, null);

            assertThatThrownBy(() -> new SalesReport(orders)).isInstanceOf(NullPointerException.class);
        }

        @ParameterizedTest(name = "threshold {0}")
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        @DisplayName("partitionByTotal() rejects a threshold below 1")
        void partitionRejectsInvalidThreshold(long threshold) {
            assertThatThrownBy(() -> report.partitionByTotal(threshold))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
