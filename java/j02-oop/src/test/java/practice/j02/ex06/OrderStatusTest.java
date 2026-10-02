package practice.j02.ex06;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static practice.j02.ex06.OrderStatus.CANCELLED;
import static practice.j02.ex06.OrderStatus.DELIVERED;
import static practice.j02.ex06.OrderStatus.PAID;
import static practice.j02.ex06.OrderStatus.PLACED;
import static practice.j02.ex06.OrderStatus.SHIPPED;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("J02-06 · Enums with behavior")
class OrderStatusTest {

    /** The diagram in the Javadoc of OrderStatus, as "FROM>TO" strings. */
    private static final Set<String> ALLOWED_MOVES =
            Set.of("PLACED>PAID", "PLACED>CANCELLED", "PAID>SHIPPED", "PAID>CANCELLED", "SHIPPED>DELIVERED");

    @Nested
    @DisplayName("Basic")
    class Basic {

        @ParameterizedTest(name = "{0} → {1}")
        @CsvSource({"PLACED, PAID", "PLACED, CANCELLED", "PAID, SHIPPED", "PAID, CANCELLED", "SHIPPED, DELIVERED"})
        @DisplayName("allows each move in the life cycle")
        void allowedMoves(OrderStatus from, OrderStatus to) {
            assertThat(from.canTransitionTo(to)).isTrue();
            assertThat(from.transitionTo(to)).isEqualTo(to);
        }

        @Test
        @DisplayName("allowedNext() lists the statuses each status can move to")
        void allowedNext() {
            assertThat(PLACED.allowedNext()).containsExactlyInAnyOrder(PAID, CANCELLED);
            assertThat(PAID.allowedNext()).containsExactlyInAnyOrder(SHIPPED, CANCELLED);
            assertThat(SHIPPED.allowedNext()).containsExactly(DELIVERED);
        }

        @Test
        @DisplayName("an order can go all the way from PLACED to DELIVERED")
        void happyPath() {
            OrderStatus status = PLACED;

            status = status.transitionTo(PAID);
            status = status.transitionTo(SHIPPED);
            status = status.transitionTo(DELIVERED);

            assertThat(status).isEqualTo(DELIVERED);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("allows exactly the moves in the diagram, and no others (all 25 pairs)")
        void exactlyTheDiagram() {
            for (OrderStatus from : OrderStatus.values()) {
                for (OrderStatus to : OrderStatus.values()) {
                    boolean expected = ALLOWED_MOVES.contains(from + ">" + to);

                    assertThat(from.canTransitionTo(to)).as("%s → %s", from, to).isEqualTo(expected);
                }
            }
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(OrderStatus.class)
        @DisplayName("no status can move to itself")
        void notToItself(OrderStatus status) {
            assertThat(status.canTransitionTo(status)).isFalse();
        }

        @Test
        @DisplayName("DELIVERED and CANCELLED are final: nothing comes after them")
        void finalStatuses() {
            assertThat(DELIVERED.allowedNext()).isEmpty();
            assertThat(CANCELLED.allowedNext()).isEmpty();
        }

        @Test
        @DisplayName("allowedNext() returns a set that can't be modified")
        void allowedNextUnmodifiable() {
            var next = PLACED.allowedNext();

            assertThatThrownBy(() -> next.add(DELIVERED)).isInstanceOf(UnsupportedOperationException.class);
            assertThat(PLACED.canTransitionTo(DELIVERED)).isFalse();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("transitionTo() refuses a move that isn't allowed and names both statuses")
        void refusedMove() {
            assertThatThrownBy(() -> DELIVERED.transitionTo(PAID))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("DELIVERED")
                    .hasMessageContaining("PAID");
        }

        @Test
        @DisplayName("an order that has shipped can't be cancelled any more")
        void shippedCantBeCancelled() {
            assertThatThrownBy(() -> SHIPPED.transitionTo(CANCELLED)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("an order can't skip a step")
        void cantSkipSteps() {
            assertThatThrownBy(() -> PLACED.transitionTo(SHIPPED)).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("rejects null")
        void rejectsNull() {
            assertThatThrownBy(() -> PLACED.canTransitionTo(null)).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> PLACED.transitionTo(null)).isInstanceOf(NullPointerException.class);
        }
    }
}
