package com.example.bookstore.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.Category;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

@DisplayName("Template · Inventory")
class InventoryTest {

    @Nested
    @DisplayName("decrease")
    class Decrease {

        @Test
        @DisplayName("takes copies out of stock")
        void takesCopies() {
            Inventory inventory = Inventory.of(book(7L), 10);

            inventory.decrease(3);

            assertThat(inventory.getQuantity()).isEqualTo(7);
        }

        @Test
        @DisplayName("can take the last copy")
        void takesTheLastCopy() {
            Inventory inventory = Inventory.of(book(7L), 3);

            inventory.decrease(3);

            assertThat(inventory.getQuantity()).isZero();
            assertThat(inventory.isInStock()).isFalse();
        }

        @Test
        @DisplayName("rejects more copies than are left with OUT_OF_STOCK, and keeps the stock")
        void rejectsMoreThanLeft() {
            Inventory inventory = Inventory.of(book(7L), 3);

            assertThatThrownBy(() -> inventory.decrease(5))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Only 3 copies of book 7 are left, but 5 were requested.")
                    .extracting("errorCode").isEqualTo(ErrorCode.OUT_OF_STOCK);
            assertThat(inventory.getQuantity()).isEqualTo(3);
        }

        @ParameterizedTest(name = "quantity {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("rejects a quantity below 1")
        void rejectsNonPositive(int quantity) {
            Inventory inventory = Inventory.of(book(7L), 3);

            assertThatThrownBy(() -> inventory.decrease(quantity)).isInstanceOf(IllegalArgumentException.class);
            assertThat(inventory.getQuantity()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("increase")
    class Increase {

        @Test
        @DisplayName("puts copies back into stock")
        void addsCopies() {
            Inventory inventory = Inventory.of(book(7L), 0);

            inventory.increase(4);

            assertThat(inventory.getQuantity()).isEqualTo(4);
            assertThat(inventory.isInStock()).isTrue();
        }

        @ParameterizedTest(name = "quantity {0}")
        @ValueSource(ints = {0, -1})
        @DisplayName("rejects a quantity below 1")
        void rejectsNonPositive(int quantity) {
            Inventory inventory = Inventory.of(book(7L), 3);

            assertThatThrownBy(() -> inventory.increase(quantity)).isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("rejects an increase that would overflow")
        void rejectsOverflow() {
            Inventory inventory = Inventory.of(book(7L), Integer.MAX_VALUE);

            assertThatThrownBy(() -> inventory.increase(1)).isInstanceOf(ArithmeticException.class);
            assertThat(inventory.getQuantity()).isEqualTo(Integer.MAX_VALUE);
        }
    }

    @Test
    @DisplayName("of() shares the book's id and rejects a negative quantity")
    void creation() {
        assertThat(Inventory.of(book(7L), 0).getBookId()).isEqualTo(7L);
        assertThatThrownBy(() -> Inventory.of(book(7L), -1)).isInstanceOf(IllegalArgumentException.class);
    }

    /** A book as if it was loaded from the database, so it has an id. */
    private static Book book(long id) {
        Book book = Book.create("9791115881394", "Effective Java", "Joshua Bloch", Category.create("Programming"),
                36_000, LocalDate.of(2018, 10, 18));
        ReflectionTestUtils.setField(book, "id", id);
        return book;
    }
}
