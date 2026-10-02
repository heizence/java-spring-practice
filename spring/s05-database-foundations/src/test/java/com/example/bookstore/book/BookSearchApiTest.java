package com.example.bookstore.book;

import static com.example.bookstore.book.BookTestData.insertBook;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * {@code GET /api/books/search} end to end, including the requests an attacker would send. Each test runs in a
 * transaction that is rolled back, so the books it inserts disappear afterwards. No real book's title or author
 * contains "Quokka", so a search for it finds only the books inserted here.
 */
@IntegrationTest
@DisplayName("S05-03 · SQL injection")
class BookSearchApiTest {

    private static final long PROGRAMMING = 1L;
    private static final long FICTION = 4L;

    /**
     * Reads every member's email and password hash, if the text ever becomes part of the SQL. The quote ends the
     * string, the UNION adds the members' rows, and {@code -- -} turns the rest of the SQL into a comment (the last
     * dash keeps the comment's space from being trimmed away).
     */
    private static final String UNION_ATTACK =
            "x' UNION SELECT id, email, password_hash, name, role, 0, DATE(created_at) FROM members -- -";

    /** The same attack with a backslash before the quote: MySQL reads {@code \'} as an escaped quote. */
    private static final String BACKSLASH_ATTACK = UNION_ATTACK.replace("x'", "x\\'");

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @BeforeEach
    void insertQuokkaBooks() {
        insertBook(jdbc, PROGRAMMING, "The Quokka Way", "Ann O'Neil", 30_000, LocalDate.of(2020, 3, 1));
        insertBook(jdbc, PROGRAMMING, "Quokka Patterns", "Ben Kim", 20_000, LocalDate.of(2022, 6, 1));
        insertBook(jdbc, FICTION, "Island Life", "Cara Quokka", 25_000, LocalDate.of(2018, 9, 1));
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("finds the books whose title or author contains the text, ignoring case, sorted by title")
        void findsByTitleOrAuthor() {
            assertThat(search("QUOKKA", null))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].title").asArray()
                    .containsExactly("Island Life", "Quokka Patterns", "The Quokka Way");
        }

        @Test
        @DisplayName("returns each book as JSON, with its category's name")
        void returnsJson() {
            assertThat(search("quokka patterns", null))
                    .hasStatusOk()
                    .bodyJson().isLenientlyEqualTo("""
                            [ { "title": "Quokka Patterns", "author": "Ben Kim", "category": "Programming",
                                "price": 20000, "publishedDate": "2022-06-01" } ]""");
        }

        @Test
        @DisplayName("sorts by price, highest first, with sort=price,desc")
        void sortsByPriceDescending() {
            assertThat(search("quokka", "price,desc"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].title").asArray()
                    .containsExactly("The Quokka Way", "Island Life", "Quokka Patterns");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("finds a title with an apostrophe")
        void titleWithAnApostrophe() {
            insertBook(jdbc, FICTION, "The Quokka's Guide to the Galaxy", "Dan Park", 15_000, LocalDate.of(1979, 10, 12));

            assertThat(search("Quokka's Guide", null))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].title").asArray()
                    .containsExactly("The Quokka's Guide to the Galaxy");
        }

        @Test
        @DisplayName("finds an author with an apostrophe")
        void authorWithAnApostrophe() {
            assertThat(search("O'Neil", null))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].title").asArray()
                    .containsExactly("The Quokka Way");
        }

        @Test
        @DisplayName("sorts by publishedDate, the API's name for the published_date column")
        void sortsByPublishedDate() {
            assertThat(search("quokka", "publishedDate"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].title").asArray()
                    .containsExactly("Island Life", "The Quokka Way", "Quokka Patterns");
        }

        @Test
        @DisplayName("accepts the direction in any letter case")
        void directionIgnoresCase() {
            assertThat(search("quokka", "price,DESC"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].title").asArray()
                    .containsExactly("The Quokka Way", "Island Life", "Quokka Patterns");
        }

        @Test
        @DisplayName("sorts books with the same price by id")
        void samePriceSortedById() {
            insertBook(jdbc, FICTION, "Quokka Twins", "Eve Cho", 20_000, LocalDate.of(2021, 1, 1));

            assertThat(search("quokka", "price"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].title").asArray()
                    .containsExactly("Quokka Patterns", "Quokka Twins", "Island Life", "The Quokka Way");
        }

        @Test
        @DisplayName("returns at most 20 books")
        void returnsAtMost20Books() {
            for (int i = 1; i <= 18; i++) {
                insertBook(jdbc, FICTION, "Quokka Tale " + i, "Fay Oh", 10_000 + i, LocalDate.of(2020, 1, i));
            }

            assertThat(search("quokka", null))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$").asArray().hasSize(BookSearchRepository.MAX_RESULTS);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "sort={0}", quoteTextArguments = false)
        @ValueSource(strings = {"colour", "isbn", "published_date", "price,sideways", "price,", "price,desc,asc"})
        @DisplayName("rejects a sort that the API doesn't offer with 400 INVALID_INPUT")
        void rejectsUnknownSorts(String sort) {
            assertThat(search("quokka", sort))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("INVALID_INPUT"))
                    .hasPathSatisfying("$.detail", detail -> assertThat(detail).asString().contains(sort));
        }

        @Test
        @DisplayName("rejects a blank search text with 400 INVALID_INPUT")
        void rejectsABlankText() {
            assertThat(search("  ", null))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("INVALID_INPUT"));
        }
    }

    @Nested
    @DisplayName("Injection attempts")
    class InjectionAttempts {

        @Test
        @DisplayName("a quote in the text can't add OR '1'='1' to the query")
        void quoteCantAddAnOrCondition() {
            assertFindsNothing("' OR '1'='1");
        }

        @Test
        @DisplayName("a UNION in the text can't read the members table")
        void unionCantReadMembers() {
            assertFindsNothing(UNION_ATTACK);
        }

        @Test
        @DisplayName("a backslash before the quote doesn't help the UNION either")
        void backslashDoesntHelp() {
            assertFindsNothing(BACKSLASH_ATTACK);
        }

        /** No book contains the attack's text. The titles in the failure message show what leaked instead. */
        private void assertFindsNothing(String attack) {
            assertThat(search(attack, null))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$[*].title").asArray().as("titles found").isEmpty();
        }

        @Test
        @DisplayName("a subquery in sort is rejected with 400, not run")
        void subqueryInSortIsRejected() {
            String probe = "(CASE WHEN (SELECT password_hash FROM members WHERE id = 20) LIKE '{bcrypt}%'"
                    + " THEN b.title ELSE b.price END)";

            assertThat(search("quokka", probe)).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("SQL after the sort direction is rejected with 400, not run")
        void sqlAfterTheDirectionIsRejected() {
            assertThat(search("quokka", "price,desc LIMIT 1 -- ")).hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    /** {@code GET /api/books/search?q=…&sort=…}; without {@code sort} when it's {@code null}. */
    private MvcTestResult search(String text, String sort) {
        var request = mvc.get().uri("/api/books/search").param("q", text);
        if (sort != null) {
            request = request.param("sort", sort);
        }
        return request.exchange();
    }
}
