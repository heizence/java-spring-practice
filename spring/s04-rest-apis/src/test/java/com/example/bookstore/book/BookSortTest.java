package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.example.bookstore.book.BookSort.Direction;
import com.example.bookstore.book.BookSort.Property;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** {@link BookSort#parse} on its own: the {@code sort} parameter, without HTTP. */
@DisplayName("S04-06 · Pagination and sorting")
class BookSortTest {

    @Nested
    @DisplayName("Basic")
    class Basic {

        @ParameterizedTest(name = "sort={0}")
        @CsvSource(delimiter = ';', textBlock = """
                id;            ID
                title;         TITLE
                author;        AUTHOR
                price;         PRICE
                publishedDate; PUBLISHED_DATE""")
        @DisplayName("a property alone sorts ascending")
        void propertyAlone(String parameter, Property property) {
            assertThat(BookSort.parse(parameter)).isEqualTo(new BookSort(property, Direction.ASC));
        }

        @Test
        @DisplayName("price,desc sorts by price, descending")
        void descending() {
            assertThat(BookSort.parse("price,desc")).isEqualTo(new BookSort(Property.PRICE, Direction.DESC));
        }

        @Test
        @DisplayName("publishedDate,asc sorts by publication date, ascending")
        void ascending() {
            assertThat(BookSort.parse("publishedDate,asc"))
                    .isEqualTo(new BookSort(Property.PUBLISHED_DATE, Direction.ASC));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @ParameterizedTest(name = "sort={0}")
        @ValueSource(strings = {"price,DESC", "price,Desc", "price , desc", " price,desc "})
        @DisplayName("the direction may have any case, and spaces around the parts don't matter")
        void caseAndSpaces(String parameter) {
            assertThat(BookSort.parse(parameter)).isEqualTo(new BookSort(Property.PRICE, Direction.DESC));
        }

        @Test
        @DisplayName("id is the default order")
        void defaultOrder() {
            assertThat(BookSort.parse("id")).isEqualTo(BookSort.DEFAULT);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an unknown property is invalid input, and the message lists the allowed ones")
        void unknownProperty() {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> BookSort.parse("colour"))
                    .withMessage("Can't sort by 'colour'. Sort by one of: id, title, author, price, publishedDate.")
                    .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @ParameterizedTest(name = "sort={0}")
        @ValueSource(strings = {"PRICE", "Price", "published_date", "isbn", "category"})
        @DisplayName("properties are spelled exactly like the JSON properties, and only allowed ones work")
        void notAnAllowedProperty(String parameter) {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> BookSort.parse(parameter))
                    .withMessageStartingWith("Can't sort by '" + parameter + "'.")
                    .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @Test
        @DisplayName("an unknown direction is invalid input")
        void unknownDirection() {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> BookSort.parse("price,sideways"))
                    .withMessage("Can't sort in direction 'sideways'. Use asc or desc.")
                    .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }

        @ParameterizedTest(name = "sort={0}")
        @ValueSource(strings = {"price,desc,id", ",desc", "price,", ",", "", "price,,desc"})
        @DisplayName("anything else is invalid input that shows the whole parameter")
        void malformed(String parameter) {
            assertThatExceptionOfType(BusinessException.class)
                    .isThrownBy(() -> BookSort.parse(parameter))
                    .withMessage("Can't read the sort '%s'. Use a property and an optional direction, such as "
                            + "price,desc.", parameter)
                    .satisfies(e -> assertThat(e.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT));
        }
    }
}
