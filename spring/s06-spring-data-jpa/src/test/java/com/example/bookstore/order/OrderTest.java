package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;

import java.time.Instant;
import java.time.LocalDate;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.Category;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

/** The {@link Order} aggregate's own rules, without Spring or a database. */
@DisplayName("S06-03 · Relationships and aggregates")
class OrderTest {

    private static final Instant ORDERED_AT = Instant.parse("2026-10-15T00:00:00Z");

    private final Book effectiveJava = book(1L, 36_000);
    private final Book cleanCode = book(2L, 12_000);
    private final Order order = Order.place(Member.register("minji@example.com", "Minji Kim", "{noop}x"), ORDERED_AT);

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("addLine() adds a line at the book's current price and updates the total")
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
        @DisplayName("each new line points back to its order (the side that JPA saves as order_id)")
        void linksLinesToTheirOrder() {
            order.addLine(effectiveJava, 1);
            order.addLine(cleanCode, 1);

            assertThat(order.getLines()).allSatisfy(line -> assertThat(line.getOrder()).isSameAs(order));
        }

        @Test
        @DisplayName("removeLine() removes the book's line and updates the total")
        void removesLines() {
            order.addLine(effectiveJava, 2);
            order.addLine(cleanCode, 1);

            order.removeLine(effectiveJava);

            assertThat(order.getLines()).extracting(OrderLine::getBook).containsExactly(cleanCode);
            assertThat(order.getTotalAmount()).isEqualTo(12_000);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("adding a book again adds to its line instead of creating a second line")
        void mergesTheSameBook() {
            order.addLine(effectiveJava, 2);

            order.addLine(book(1L, 36_000), 3); // the same book, loaded again as another object

            assertThat(order.getLines()).singleElement().returns(5, OrderLine::getQuantity);
            assertThat(order.getTotalAmount()).isEqualTo(180_000);
        }

        @Test
        @DisplayName("removing a book that isn't in the order changes nothing")
        void removesAnUnknownBook() {
            order.addLine(effectiveJava, 1);

            order.removeLine(cleanCode);

            assertThat(order.getLines()).extracting(OrderLine::getBook).containsExactly(effectiveJava);
            assertThat(order.getTotalAmount()).isEqualTo(36_000);
        }

        @Test
        @DisplayName("removing the last line leaves an empty order with a total of 0")
        void removesTheLastLine() {
            order.addLine(effectiveJava, 1);

            order.removeLine(effectiveJava);

            assertThat(order.getLines()).isEmpty();
            assertThat(order.getTotalAmount()).isZero();
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }

        @Test
        @DisplayName("a line keeps the price the book had when it was ordered")
        void keepsThePriceAtTheTimeOfOrdering() {
            order.addLine(effectiveJava, 1);

            effectiveJava.changePrice(40_000);

            assertThat(order.getLines().getFirst().getUnitPrice()).isEqualTo(36_000);
            assertThat(order.getTotalAmount()).isEqualTo(36_000);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("rejects a quantity below 1 or a missing book with IllegalArgumentException, and adds nothing")
        void rejectsInvalidLines() {
            assertThatThrownBy(() -> order.addLine(effectiveJava, 0)).isInstanceOf(IllegalArgumentException.class);
            assertThatThrownBy(() -> order.addLine(null, 1)).isInstanceOf(IllegalArgumentException.class);

            assertThat(order.getLines()).isEmpty();
            assertThat(order.getTotalAmount()).isZero();
        }

        @Test
        @DisplayName("the lines of a paid order can't change: INVALID_ORDER_STATUS, and the order stays as it was")
        void rejectsChangesAfterPayment() {
            order.addLine(effectiveJava, 1);
            order.markPaid();

            assertThatThrownBy(() -> order.addLine(cleanCode, 1))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("The new order is PAID; its lines can only change while it is PENDING.")
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
            assertThatThrownBy(() -> order.removeLine(effectiveJava))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ORDER_STATUS);

            assertThat(order.getLines()).extracting(OrderLine::getBook).containsExactly(effectiveJava);
            assertThat(order.getTotalAmount()).isEqualTo(36_000);
        }

        @Test
        @DisplayName("the lines of a cancelled order can't change either")
        void rejectsChangesAfterCancelling() {
            order.addLine(effectiveJava, 1);
            order.cancel();

            assertThatThrownBy(() -> order.removeLine(effectiveJava))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.INVALID_ORDER_STATUS);
            assertThat(order.getLines()).hasSize(1);
        }
    }

    /** A book as if it was loaded from the database, so it has an id. */
    private static Book book(long id, long price) {
        Book book = Book.create("97911" + "%08d".formatted(id), "Book " + id, "Author", Category.create("Programming"),
                price, LocalDate.of(2020, 1, 1));
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
