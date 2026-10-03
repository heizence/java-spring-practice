package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import com.example.bookstore.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * {@code GET /api/books?page&size&sort} over HTTP. The tests check the order itself (sorted by the property, equal
 * values by id), so they don't depend on the price or title of any particular book.
 */
@IntegrationTest
@DisplayName("S04-06 · Pagination and sorting")
class BookPagingApiTest {

    /** By price, then by id: the order of ?sort=price. */
    private static final Comparator<Listed> BY_PRICE_THEN_ID =
            Comparator.comparingLong(Listed::price).thenComparingLong(Listed::id);

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("?sort=price,desc: the most expensive books first, equal prices in id order")
        void byPriceDescending() {
            List<Listed> books = booksOf(get("/api/books?sort=price,desc"));

            assertThat(books).hasSize(20)
                    .isSortedAccordingTo(Comparator.comparingLong(Listed::price).reversed()
                            .thenComparingLong(Listed::id));
        }

        @Test
        @DisplayName("?sort=price: the cheapest books first, equal prices in id order")
        void byPriceAscending() {
            List<Listed> books = booksOf(get("/api/books?sort=price"));

            assertThat(books).isSortedAccordingTo(BY_PRICE_THEN_ID);
        }

        @Test
        @DisplayName("?sort=title: titles in alphabetical order")
        void byTitle() {
            List<String> titles = valuesOf(get("/api/books?sort=title&size=100"), "$.content[*].title");

            assertThat(titles).hasSize(60).isSorted();
        }

        @Test
        @DisplayName("?sort=publishedDate,desc: the newest books first")
        void byPublishedDateDescending() {
            List<String> dates = valuesOf(get("/api/books?sort=publishedDate,desc&size=100"),
                    "$.content[*].publishedDate");

            assertThat(dates).hasSize(60).isSortedAccordingTo(Comparator.reverseOrder());
        }

        @Test
        @DisplayName("sorting comes before paging: page 1 continues where page 0 ended")
        void sortsBeforePaging() {
            List<Listed> firstPage = booksOf(get("/api/books?sort=price&size=20&page=0"));
            List<Listed> secondPage = booksOf(get("/api/books?sort=price&size=20&page=1"));

            var both = new ArrayList<>(firstPage);
            both.addAll(secondPage);
            assertThat(both).isSortedAccordingTo(BY_PRICE_THEN_ID);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("with equal prices on page boundaries, every book is on exactly one page")
        void everyBookOnExactlyOnePage() {
            List<Long> ids = new ArrayList<>();
            for (int page = 0; page < 9; page++) {
                booksOf(get("/api/books?sort=price,desc&size=7&page=" + page)).forEach(listed -> ids.add(listed.id()));
            }

            assertThat(ids).containsExactlyInAnyOrderElementsOf(IntStream.rangeClosed(1, 60).mapToObj(id -> (long) id)
                    .toList());
        }

        @Test
        @DisplayName("?size=100, the largest size, answers all 60 books")
        void largestSize() {
            assertThat(mvc.get().uri("/api/books?size=100"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content", content -> assertThat(content).asArray().hasSize(60))
                    .hasPathSatisfying("$.page.size", size -> assertThat(size).isEqualTo(100));
        }

        @Test
        @DisplayName("?size=1&page=59 answers the last book alone")
        void smallestSize() {
            assertThat(mvc.get().uri("/api/books?size=1&page=59"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content[*].id", ids -> assertThat(ids).asArray().containsExactly(60))
                    .hasPathSatisfying("$.page.totalPages", pages -> assertThat(pages).isEqualTo(60));
        }

        @Test
        @DisplayName("an empty ?sort= sorts by id, like no sort at all")
        void emptySort() {
            assertThat(booksOf(get("/api/books?sort=")).stream().map(Listed::id).toList())
                    .containsExactlyElementsOf(IntStream.rangeClosed(1, 20).mapToObj(id -> (long) id).toList());
        }

        @Test
        @DisplayName("?sort=author,DESC: the direction may be in capitals")
        void directionInCapitals() {
            List<String> authors = valuesOf(get("/api/books?sort=author,DESC&size=100"), "$.content[*].author");

            assertThat(authors).isSortedAccordingTo(Comparator.reverseOrder());
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("?size=101 is more than the API allows: 400, with the parameter in errors")
        void sizeTooLarge() {
            assertThat(mvc.get().uri("/api/books?size=101"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Validation failure",
                              "instance": "/api/books",
                              "code": "INVALID_INPUT",
                              "errors": [ { "field": "size", "message": "must be less than or equal to 100" } ]
                            }""");
        }

        @Test
        @DisplayName("?size=0 and ?page=-1 are invalid too, and both are listed")
        void sizeZeroAndNegativePage() {
            assertThat(mvc.get().uri("/api/books?size=0&page=-1"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.errors").isEqualTo(List.of(
                            Map.of("field", "page", "message", "must be greater than or equal to 0"),
                            Map.of("field", "size", "message", "must be greater than or equal to 1")));
        }

        @Test
        @DisplayName("?sort=colour: 400, and the detail says which properties are allowed")
        void unknownSortProperty() {
            assertThat(mvc.get().uri("/api/books?sort=colour"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "%s",
                              "instance": "/api/books",
                              "code": "INVALID_INPUT"
                            }""".formatted(
                            "Can't sort by 'colour'. Sort by one of: id, title, author, price, publishedDate."));
        }

        @Test
        @DisplayName("?sort=price,sideways: 400")
        void unknownDirection() {
            assertThat(mvc.get().uri("/api/books?sort=price,sideways"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.detail")
                    .isEqualTo("Can't sort in direction 'sideways'. Use asc or desc.");
        }

        @Test
        @DisplayName("?sort=price,desc,id: 400, only one property at a time")
        void tooManyParts() {
            assertThat(mvc.get().uri("/api/books?sort=price,desc,id"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT");
        }
    }

    /** The two values of a listed book that the order depends on. */
    record Listed(long id, long price) {
    }

    private MvcTestResult get(String uri) {
        MvcTestResult result = mvc.get().uri(uri).exchange();
        assertThat(result).hasStatusOk();
        return result;
    }

    private static List<Listed> booksOf(MvcTestResult result) {
        List<Number> ids = valuesOf(result, "$.content[*].id");
        List<Number> prices = valuesOf(result, "$.content[*].price");
        return IntStream.range(0, ids.size())
                .mapToObj(i -> new Listed(ids.get(i).longValue(), prices.get(i).longValue()))
                .toList();
    }

    private static <T> List<T> valuesOf(MvcTestResult result, String jsonPath) {
        try {
            return JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), jsonPath);
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
