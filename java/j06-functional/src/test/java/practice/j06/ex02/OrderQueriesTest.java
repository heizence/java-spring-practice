package practice.j06.ex02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static practice.j06.ex02.OrderStatus.CANCELLED;
import static practice.j06.ex02.OrderStatus.NEW;
import static practice.j06.ex02.OrderStatus.PAID;
import static practice.j06.ex02.OrderStatus.SHIPPED;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J06-02 · Streams basics")
class OrderQueriesTest {

    private static final Order A1 = new Order("A-1", "mina", PAID, 12_000);
    private static final Order A2 = new Order("A-2", "jun", NEW, 5_000);
    private static final Order A3 = new Order("A-3", "mina", SHIPPED, 30_000);
    private static final Order A4 = new Order("A-4", "ari", CANCELLED, 50_000);
    private static final Order A5 = new Order("A-5", "jun", PAID, 12_000);

    /** Revenue without the cancelled A-4: 12,000 + 5,000 + 30,000 + 12,000 = 59,000 cents. */
    private static final List<Order> ORDERS = List.of(A1, A2, A3, A4, A5);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("withStatus() keeps the orders with that status, in their original order")
        void withStatus() {
            assertThat(OrderQueries.withStatus(ORDERS, PAID)).containsExactly(A1, A5);
        }

        @Test
        @DisplayName("topByTotal() returns the orders with the highest totals, highest first")
        void topByTotal() {
            assertThat(OrderQueries.topByTotal(ORDERS, 3)).containsExactly(A4, A3, A1);
        }

        @Test
        @DisplayName("customers() lists each customer once, alphabetically")
        void customers() {
            assertThat(OrderQueries.customers(ORDERS)).containsExactly("ari", "jun", "mina");
        }

        @Test
        @DisplayName("hasOrderOver() is true when an order is over the threshold")
        void hasOrderOverTrue() {
            assertThat(OrderQueries.hasOrderOver(ORDERS, 29_999)).isTrue();
        }

        @Test
        @DisplayName("hasOrderOver() is false when no order is over the threshold")
        void hasOrderOverFalse() {
            assertThat(OrderQueries.hasOrderOver(ORDERS, 60_000)).isFalse();
        }

        @Test
        @DisplayName("revenueCents() adds up the totals of the orders that aren't cancelled")
        void revenue() {
            assertThat(OrderQueries.revenueCents(ORDERS)).isEqualTo(59_000);
        }

        @Test
        @DisplayName("averageOrderValue() averages the totals of the orders that aren't cancelled")
        void averageOrderValue() {
            assertThat(OrderQueries.averageOrderValue(ORDERS)).isEqualTo(14_750.0);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("withStatus() returns an empty list when no order has the status")
        void withStatusNoMatch() {
            var orders = List.of(A1, A2);

            assertThat(OrderQueries.withStatus(orders, SHIPPED)).isEmpty();
        }

        @Test
        @DisplayName("topByTotal() sorts orders with the same total by id, whatever the input order")
        void topByTotalBreaksTiesById() {
            var b3 = new Order("B-3", "ari", PAID, 100);
            var b1 = new Order("B-1", "jun", PAID, 100);
            var b2 = new Order("B-2", "mina", PAID, 100);

            assertThat(OrderQueries.topByTotal(List.of(b3, b1, b2), 2)).containsExactly(b1, b2);
        }

        @Test
        @DisplayName("topByTotal() with n = 0 returns an empty list")
        void topByTotalZero() {
            assertThat(OrderQueries.topByTotal(ORDERS, 0)).isEmpty();
        }

        @Test
        @DisplayName("topByTotal() with n larger than the list returns every order, sorted")
        void topByTotalMoreThanAvailable() {
            assertThat(OrderQueries.topByTotal(ORDERS, 10)).containsExactly(A4, A3, A1, A5, A2);
        }

        @Test
        @DisplayName("topByTotal() doesn't change the list it gets")
        void topByTotalLeavesInputAlone() {
            var orders = new ArrayList<>(ORDERS);

            OrderQueries.topByTotal(orders, 2);

            assertThat(orders).containsExactlyElementsOf(ORDERS);
        }

        @Test
        @DisplayName("customers() of no orders is an empty list")
        void customersEmpty() {
            assertThat(OrderQueries.customers(List.of())).isEmpty();
        }

        @Test
        @DisplayName("hasOrderOver() is false for a total equal to the threshold: it must be greater")
        void hasOrderOverIsStrict() {
            assertThat(OrderQueries.hasOrderOver(ORDERS, 50_000)).isFalse();
        }

        @Test
        @DisplayName("hasOrderOver() looks at cancelled orders too")
        void hasOrderOverCountsCancelled() {
            assertThat(OrderQueries.hasOrderOver(ORDERS, 49_999)).isTrue();
        }

        @Test
        @DisplayName("hasOrderOver() is false for no orders")
        void hasOrderOverEmpty() {
            assertThat(OrderQueries.hasOrderOver(List.of(), 0)).isFalse();
        }

        @Test
        @DisplayName("revenueCents() is 0 for no orders and for only cancelled orders")
        void revenueWithoutCountingOrders() {
            assertThat(OrderQueries.revenueCents(List.of())).isZero();
            assertThat(OrderQueries.revenueCents(List.of(A4))).isZero();
        }

        @Test
        @DisplayName("averageOrderValue() keeps the fraction: totals of 1 and 2 cents average 1.5")
        void averageKeepsFraction() {
            var orders = List.of(new Order("C-1", "ari", PAID, 1), new Order("C-2", "jun", PAID, 2));

            assertThat(OrderQueries.averageOrderValue(orders)).isEqualTo(1.5);
        }

        @Test
        @DisplayName("averageOrderValue() is 0.0 for no orders and for only cancelled orders")
        void averageWithoutCountingOrders() {
            assertThat(OrderQueries.averageOrderValue(List.of())).isZero();
            assertThat(OrderQueries.averageOrderValue(List.of(A4))).isZero();
        }

        @Test
        @DisplayName("returned lists can't be modified")
        void resultsAreUnmodifiable() {
            List<Order> paid = OrderQueries.withStatus(ORDERS, PAID);
            List<Order> top = OrderQueries.topByTotal(ORDERS, 2);
            List<String> customers = OrderQueries.customers(ORDERS);

            assertThatThrownBy(() -> paid.add(A2)).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> top.add(A2)).isInstanceOf(UnsupportedOperationException.class);
            assertThatThrownBy(() -> customers.add("sora")).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("every query rejects a null list")
        void rejectsNullList() {
            assertThatThrownBy(() -> OrderQueries.withStatus(null, PAID)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> OrderQueries.topByTotal(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> OrderQueries.customers(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> OrderQueries.hasOrderOver(null, 1)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> OrderQueries.revenueCents(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> OrderQueries.averageOrderValue(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("withStatus() rejects a null status")
        void withStatusRejectsNullStatus() {
            assertThatThrownBy(() -> OrderQueries.withStatus(ORDERS, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("topByTotal() rejects a negative n")
        void topByTotalRejectsNegativeN() {
            assertThatThrownBy(() -> OrderQueries.topByTotal(ORDERS, -1))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("-1");
        }
    }
}
