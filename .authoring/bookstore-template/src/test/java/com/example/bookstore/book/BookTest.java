package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Template · Book")
class BookTest {

    private final Book book = Book.create("9791115881394", "Effective Java", "Joshua Bloch",
            Category.create("Programming"), 36_000, LocalDate.of(2018, 10, 18));

    @Test
    @DisplayName("changePrice() sets a new price")
    void changesThePrice() {
        book.changePrice(38_000);

        assertThat(book.getPrice()).isEqualTo(38_000);
    }

    @ParameterizedTest(name = "price {0}")
    @ValueSource(longs = {0, -1})
    @DisplayName("rejects a price below 1, and keeps the old one")
    void rejectsNonPositivePrices(long price) {
        assertThatThrownBy(() -> book.changePrice(price)).isInstanceOf(IllegalArgumentException.class);
        assertThat(book.getPrice()).isEqualTo(36_000);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"979111588139", "97911158813945", "979-11-15881-39-4"})
    @DisplayName("rejects an ISBN that isn't 13 digits")
    void rejectsMalformedIsbns(String isbn) {
        assertThatThrownBy(() -> Book.create(isbn, "Effective Java", "Joshua Bloch", Category.create("Programming"),
                36_000, LocalDate.of(2018, 10, 18)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
