package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.LocalDate;
import java.util.function.Consumer;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.Category;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Template · Order")
class OrderTest {

    private static final Instant ORDERED_AT = Instant.parse("2026-10-15T00:00:00Z");

    private final Book effectiveJava = book(1L, 36_000);
    private final Book cleanCode = book(2L, 12_000);
    private final Order order = Order.place(Member.register("minji@example.com", "Minji Kim", "{noop}x"), ORDERED_AT);

    @Test
    @DisplayName("a new order is PENDING, without lines")
    void startsPending() {
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getOrderedAt()).isEqualTo(ORDERED_AT);
        assertThat(order.getLines()).isEmpty();
        assertThat(order.getTotalAmount()).isZero();
    }

    @Nested
    @DisplayName("Lines")
    class Lines {

        @Test
        @DisplayName("addLine() keeps the book's current price and updates the total")
        void addsLines() {
            order.addLine(effectiveJava, 2);
            order.addLine(cleanCode, 1);

            assertThat(order.getLines())
                    .extracting(OrderLine::getBook, OrderLine::getQuantity, OrderLine::getUnitPrice,
                            OrderLine::getAmount)
                    .containsExactly(tuple(effectiveJava, 2, 36_000L, 72_000L), tuple(cleanCode, 1, 12_000L, 12_000L));
            assertThat(order.getTotalAmount()).isEqualTo(84_000);
        }

        @Test
        @DisplayName("adding a book again increases its line's quantity")
        void mergesTheSameBook() {
            order.addLine(effectiveJava, 2);
            order.addLine(book(1L, 36_000), 1); // the same book, loaded again

            assertThat(order.getLines()).singleElement()
                    .returns(3, OrderLine::getQuantity);
            assertThat(order.getTotalAmount()).isEqualTo(108_000);
        }

        @Test
        @DisplayName("a later price change doesn't change an existing line")
        void keepsThePriceAtTheTimeOfOrdering() {
            order.addLine(effectiveJava, 1);

            effectiveJava.changePrice(40_000);

            assertThat(order.getLines().getFirst().getUnitPrice()).isEqualTo(36_000);
            assertThat(order.getTotalAmount()).isEqualTo(36_000);
        }

        @Test
        @DisplayName("removeLine() removes the book's line and updates the total; an unknown book changes nothing")
        void removesLines() {
            order.addLine(effectiveJava, 2);
            order.addLine(cleanCode, 1);

            order.removeLine(effectiveJava);
            order.removeLine(book(99L, 10_000));

            assertThat(order.getLines()).extracting(OrderLine::getBook).containsExactly(cleanCode);
            assertThat(order.getTotalAmount()).isEqualTo(12_000);
        }

        @Test
        @DisplayName("rejects a quantity below 1")
        void rejectsNonPositiveQuantity() {
            assertThatThrownBy(() -> order.addLine(effectiveJava, 0)).isInstanceOf(IllegalArgumentException.class);
            assertThat(order.getLines()).isEmpty();
        }

        @Test
        @DisplayName("the lines can't be changed from outside")
        void linesAreReadOnly() {
            assertThatThrownBy(() -> order.getLines().clear()).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("lines can't change once the order is paid")
        void rejectsChangesAfterPayment() {
            order.addLine(effectiveJava, 1);
            order.markPaid();

            assertThatThrownBy(() -> order.addLine(cleanCode, 1))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("The new order is PAID; its lines can only change while it is PENDING.")
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
            assertThatThrownBy(() -> order.removeLine(effectiveJava))
                    .isInstanceOf(BusinessException.class);
            assertThat(order.getLines()).hasSize(1);
            assertThat(order.getTotalAmount()).isEqualTo(36_000);
        }
    }

    @Nested
    @DisplayName("Status changes")
    class StatusChanges {

        @Test
        @DisplayName("PENDING → PAID → SHIPPED")
        void paysAndShips() {
            order.markPaid();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);

            order.ship();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPED);
        }

        @Test
        @DisplayName("a pending order can be cancelled")
        void cancelsAPendingOrder() {
            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @Test
        @DisplayName("a paid order can be cancelled")
        void cancelsAPaidOrder() {
            order.markPaid();

            order.cancel();

            assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }

        @ParameterizedTest(name = "{0} order: {1}() is rejected", quoteTextArguments = false)
        @CsvSource({
                "PENDING, ship", "PAID, markPaid",
                "SHIPPED, markPaid", "SHIPPED, ship", "SHIPPED, cancel",
                "CANCELLED, markPaid", "CANCELLED, ship", "CANCELLED, cancel"})
        @DisplayName("rejects every other change with INVALID_ORDER_STATUS, and keeps the status")
        void rejectsInvalidChanges(OrderStatus status, String change) {
            moveTo(status);

            assertThatThrownBy(() -> action(change).accept(order))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageStartingWith("The new order is " + status + ", so it can't become")
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
            assertThat(order.getStatus()).isEqualTo(status);
        }

        private void moveTo(OrderStatus status) {
            switch (status) {
                case PENDING -> { }
                case PAID -> order.markPaid();
                case SHIPPED -> {
                    order.markPaid();
                    order.ship();
                }
                case CANCELLED -> order.cancel();
            }
        }

        private static Consumer<Order> action(String change) {
            return switch (change) {
                case "markPaid" -> Order::markPaid;
                case "ship" -> Order::ship;
                case "cancel" -> Order::cancel;
                default -> throw new IllegalArgumentException(change);
            };
        }
    }

    @ParameterizedTest(name = "{0} → {1}: {2}")
    @CsvSource({
            "PENDING, PAID, true", "PENDING, SHIPPED, false", "PENDING, CANCELLED, true", "PENDING, PENDING, false",
            "PAID, SHIPPED, true", "PAID, CANCELLED, true", "PAID, PENDING, false", "PAID, PAID, false",
            "SHIPPED, PENDING, false", "SHIPPED, PAID, false", "SHIPPED, CANCELLED, false", "SHIPPED, SHIPPED, false",
            "CANCELLED, PENDING, false", "CANCELLED, PAID, false", "CANCELLED, SHIPPED, false",
            "CANCELLED, CANCELLED, false"})
    @DisplayName("OrderStatus allows exactly the documented transitions")
    void allowedTransitions(OrderStatus from, OrderStatus to, boolean allowed) {
        assertThat(from.canChangeTo(to)).isEqualTo(allowed);
    }

    /** A book as if it was loaded from the database, so it has an id. */
    private static Book book(long id, long price) {
        Book book = Book.create("97911" + "%08d".formatted(id), "Book " + id, "Author", Category.create("Programming"),
                price, LocalDate.of(2020, 1, 1));
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
