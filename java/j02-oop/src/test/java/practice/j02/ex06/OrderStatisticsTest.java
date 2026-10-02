package practice.j02.ex06;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static practice.j02.ex06.OrderStatus.CANCELLED;
import static practice.j02.ex06.OrderStatus.DELIVERED;
import static practice.j02.ex06.OrderStatus.PAID;
import static practice.j02.ex06.OrderStatus.PLACED;
import static practice.j02.ex06.OrderStatus.SHIPPED;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J02-06 · Enums with behavior")
class OrderStatisticsTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("countByStatus() counts how many orders have each status")
        void counts() {
            var statuses = List.of(PAID, PLACED, PAID, DELIVERED, PAID, CANCELLED, SHIPPED);

            var counts = OrderStatistics.countByStatus(statuses);

            assertThat(counts).containsOnly(
                    entry(PLACED, 1), entry(PAID, 3), entry(SHIPPED, 1), entry(DELIVERED, 1), entry(CANCELLED, 1));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("every status is in the result, with 0 when no order has it")
        void zeroCounts() {
            var counts = OrderStatistics.countByStatus(List.of(SHIPPED, SHIPPED));

            assertThat(counts).containsOnly(
                    entry(PLACED, 0), entry(PAID, 0), entry(SHIPPED, 2), entry(DELIVERED, 0), entry(CANCELLED, 0));
        }

        @Test
        @DisplayName("an empty list gives 0 for every status")
        void emptyList() {
            var counts = OrderStatistics.countByStatus(List.of());

            assertThat(counts).hasSize(5).allSatisfy((status, count) -> assertThat(count).isZero());
        }

        @Test
        @DisplayName("the keys come in the order the statuses are declared")
        void declarationOrder() {
            var counts = OrderStatistics.countByStatus(List.of(CANCELLED, PLACED, DELIVERED));

            assertThat(counts.keySet()).containsExactly(PLACED, PAID, SHIPPED, DELIVERED, CANCELLED);
        }

        @Test
        @DisplayName("counts 10,000 random orders correctly")
        void manyOrders() {
            var random = new Random(42); // fixed seed: a failure is reproducible
            OrderStatus[] all = OrderStatus.values();
            List<OrderStatus> statuses = new ArrayList<>();
            Map<OrderStatus, Integer> expected = new HashMap<>();
            for (int i = 0; i < 10_000; i++) {
                OrderStatus status = all[random.nextInt(all.length)];
                statuses.add(status);
                expected.merge(status, 1, Integer::sum);
            }

            var counts = OrderStatistics.countByStatus(statuses);

            for (OrderStatus status : all) {
                assertThat(counts.get(status)).as("count of %s", status).isEqualTo(expected.getOrDefault(status, 0));
            }
        }

        @Test
        @DisplayName("the result can't be modified")
        void unmodifiable() {
            var counts = OrderStatistics.countByStatus(List.of(PAID));

            assertThatThrownBy(() -> counts.put(PAID, 100)).isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a null list")
        void nullList() {
            assertThatThrownBy(() -> OrderStatistics.countByStatus(null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        @DisplayName("rejects a list that contains null")
        void nullElement() {
            List<OrderStatus> statuses = Arrays.asList(PAID, null, SHIPPED);

            assertThatThrownBy(() -> OrderStatistics.countByStatus(statuses)).isInstanceOf(NullPointerException.class);
        }
    }
}
