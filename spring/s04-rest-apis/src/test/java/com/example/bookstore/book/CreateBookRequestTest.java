package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * The validation rules of {@link CreateBookRequest}, checked directly with Bean Validation: no Spring, no HTTP. These
 * tests name the exact message of every rule, because clients read the messages in the {@code errors} of a 400
 * response (S04-03).
 */
@DisplayName("S04-02 · Request bodies and validation")
class CreateBookRequestTest {

    private static final String ISBN = "9788966261208";
    private static final String TITLE = "Effective Kotlin";
    private static final String AUTHOR = "Marcin Moskala";
    private static final long CATEGORY_ID = 1;
    private static final long PRICE = 32_000;
    private static final LocalDate PUBLISHED = LocalDate.of(2022, 3, 7);

    /** English messages, whatever the language of the computer that runs the tests. */
    private static final ValidatorFactory FACTORY = Validation.byProvider(HibernateValidator.class)
            .configure()
            .defaultLocale(Locale.ENGLISH)
            .buildValidatorFactory();

    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeFactory() {
        FACTORY.close();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a complete, valid request breaks no rule")
        void validRequest() {
            var request = new CreateBookRequest(ISBN, TITLE, AUTHOR, CATEGORY_ID, PRICE, PUBLISHED);

            assertThat(violationsOf(request)).isEmpty();
        }

        @Test
        @DisplayName("an empty request breaks every rule that requires a value, each with its message")
        void emptyRequest() {
            var request = new CreateBookRequest(null, null, null, null, null, null);

            assertThat(violationsOf(request)).containsExactly(
                    "author: must not be blank",
                    "categoryId: must not be null",
                    "isbn: must not be null",
                    "price: must not be null",
                    "publishedDate: must not be null",
                    "title: must not be blank");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a title of 200 characters is valid, one of 201 isn't")
        void titleLength() {
            assertThat(violationsOf(withTitle("t".repeat(200)))).isEmpty();
            assertThat(violationsOf(withTitle("t".repeat(201))))
                    .containsExactly("title: size must be between 0 and 200");
        }

        @Test
        @DisplayName("an author of 100 characters is valid, one of 101 isn't")
        void authorLength() {
            assertThat(violationsOf(withAuthor("a".repeat(100)))).isEmpty();
            assertThat(violationsOf(withAuthor("a".repeat(101))))
                    .containsExactly("author: size must be between 0 and 100");
        }

        @Test
        @DisplayName("a price of 1 won is valid")
        void oneWon() {
            assertThat(violationsOf(withPrice(1L))).isEmpty();
        }

        @Test
        @DisplayName("an empty ISBN reports only that it isn't 13 digits")
        void emptyIsbn() {
            assertThat(violationsOf(withIsbn(""))).containsExactly("isbn: must be 13 digits");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"978896626120", "97889662612080", "978-89-6626-120-8", "978896626120X",
                " 9788966261208"})
        @DisplayName("an ISBN that isn't exactly 13 digits is invalid")
        void isbnNotThirteenDigits(String isbn) {
            assertThat(violationsOf(withIsbn(isbn))).containsExactly("isbn: must be 13 digits");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", "   ", "\t\n"})
        @DisplayName("a blank title is invalid")
        void blankTitle(String title) {
            assertThat(violationsOf(withTitle(title))).containsExactly("title: must not be blank");
        }

        @ParameterizedTest(name = "{0}")
        @ValueSource(strings = {"", "   "})
        @DisplayName("a blank author is invalid")
        void blankAuthor(String author) {
            assertThat(violationsOf(withAuthor(author))).containsExactly("author: must not be blank");
        }

        @ParameterizedTest(name = "{0} won")
        @ValueSource(longs = {0, -1, Long.MIN_VALUE})
        @DisplayName("a price of 0 won or less is invalid")
        void priceNotPositive(long price) {
            assertThat(violationsOf(withPrice(price))).containsExactly("price: must be greater than 0");
        }
    }

    private static CreateBookRequest withIsbn(String isbn) {
        return new CreateBookRequest(isbn, TITLE, AUTHOR, CATEGORY_ID, PRICE, PUBLISHED);
    }

    private static CreateBookRequest withTitle(String title) {
        return new CreateBookRequest(ISBN, title, AUTHOR, CATEGORY_ID, PRICE, PUBLISHED);
    }

    private static CreateBookRequest withAuthor(String author) {
        return new CreateBookRequest(ISBN, TITLE, author, CATEGORY_ID, PRICE, PUBLISHED);
    }

    private static CreateBookRequest withPrice(Long price) {
        return new CreateBookRequest(ISBN, TITLE, AUTHOR, CATEGORY_ID, price, PUBLISHED);
    }

    /** The broken rules as "field: message", sorted. */
    private static List<String> violationsOf(CreateBookRequest request) {
        return VALIDATOR.validate(request).stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .sorted()
                .toList();
    }
}
