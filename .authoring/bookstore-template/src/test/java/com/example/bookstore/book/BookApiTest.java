package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Comparator;
import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SqlStatementCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** The API conventions of {@code spring/BOOKSTORE.md}, end to end: JSON, paging and problem details. */
@IntegrationTest
@DisplayName("Template · GET /api/books")
class BookApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Test
    @DisplayName("returns a book as JSON")
    void returnsABook() {
        assertThat(mvc.get().uri("/api/books/1"))
                .hasStatusOk()
                .hasContentType(MediaType.APPLICATION_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "id": 1,
                          "isbn": "9791115881394",
                          "title": "Effective Java",
                          "author": "Joshua Bloch",
                          "category": "Programming",
                          "price": 36000,
                          "publishedDate": "2018-10-18"
                        }""");
    }

    @Test
    @DisplayName("returns the first page of 20 books by default, with the page's metadata")
    void returnsTheFirstPage() {
        assertThat(mvc.get().uri("/api/books"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[*].id", ids -> assertThat(ids).asArray()
                        .containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20))
                .hasPathSatisfying("$.page", page -> assertThat(page).asMap().containsExactlyInAnyOrderEntriesOf(
                        Map.of("size", 20, "number", 0, "totalElements", 60, "totalPages", 3)));
    }

    @Test
    @DisplayName("pages and sorts as the request asks: ?page=1&size=5&sort=price,desc")
    void pagesAndSorts() {
        Comparator<Object> byPriceDescending = Comparator.comparingLong(price -> -((Number) price).longValue());

        assertThat(mvc.get().uri("/api/books?page=1&size=5&sort=price,desc"))
                .hasStatusOk()
                .bodyJson()
                .hasPathSatisfying("$.content[*].price", prices -> assertThat(prices).asArray()
                        .hasSize(5)
                        .isSortedAccordingTo(byPriceDescending))
                .hasPathSatisfying("$.page.number", number -> assertThat(number).asNumber().isEqualTo(1));
    }

    @Test
    @DisplayName("loads a page of books with their categories in two queries: the page and the count")
    void loadsAPageWithoutNPlusOneQueries() {
        mvc.get().uri("/api/books").exchange();

        assertThat(sqlStatements.count()).as("%s", sqlStatements).isEqualTo(2);
    }

    @Test
    @DisplayName("answers 404 with a problem detail for a book that doesn't exist")
    void unknownBook() {
        assertThat(mvc.get().uri("/api/books/9999"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Not Found",
                          "status": 404,
                          "detail": "Book 9999 was not found.",
                          "instance": "/api/books/9999",
                          "code": "BOOK_NOT_FOUND"
                        }""");
    }

    @Test
    @DisplayName("answers 400 with a problem detail for a sort by an unknown property")
    void unknownSortProperty() {
        assertThat(mvc.get().uri("/api/books?sort=colour,asc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Unknown property 'colour'.",
                          "instance": "/api/books",
                          "code": "INVALID_INPUT"
                        }""");
    }

    @Test
    @DisplayName("answers 400 with a problem detail for an id that isn't a number")
    void malformedId() {
        assertThat(mvc.get().uri("/api/books/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isStrictlyEqualTo("""
                        {
                          "title": "Bad Request",
                          "status": 400,
                          "detail": "Failed to convert 'id' with value: 'abc'",
                          "instance": "/api/books/abc",
                          "code": "INVALID_INPUT"
                        }""");
    }
}
