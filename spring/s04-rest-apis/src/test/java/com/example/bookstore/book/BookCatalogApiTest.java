package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.stream.IntStream;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
@DisplayName("S04-01 · Your first REST controller")
class BookCatalogApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("GET /api/books/1 answers 200 with the book as JSON")
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
        @DisplayName("GET /api/books answers the first page of 20 books, in id order, with the page's metadata")
        void returnsTheFirstPage() {
            assertThat(mvc.get().uri("/api/books"))
                    .hasStatusOk()
                    .hasContentType(MediaType.APPLICATION_JSON)
                    .bodyJson()
                    .hasPathSatisfying("$.content[*].id", ids -> assertThat(ids).asArray()
                            .containsExactly(IntStream.rangeClosed(1, 20).boxed().toArray()))
                    .hasPathSatisfying("$.page", page -> assertThat(page).asMap().containsExactlyInAnyOrderEntriesOf(
                            Map.of("size", 20, "number", 0, "totalElements", 60, "totalPages", 3)));
        }

        @Test
        @DisplayName("each book in the list has the same JSON as GET /api/books/{id}")
        void listsBooksLikeTheSingleBook() {
            assertThat(mvc.get().uri("/api/books"))
                    .bodyJson()
                    .extractingPath("$.content[0]")
                    .isEqualTo(Map.of("id", 1, "isbn", "9791115881394", "title", "Effective Java",
                            "author", "Joshua Bloch", "category", "Programming", "price", 36000,
                            "publishedDate", "2018-10-18"));
        }

        @Test
        @DisplayName("?page=2&size=7 answers the third page of 7 books: books 15 to 21")
        void pagesAsRequested() {
            assertThat(mvc.get().uri("/api/books?page=2&size=7"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content[*].id",
                            ids -> assertThat(ids).asArray().containsExactly(15, 16, 17, 18, 19, 20, 21))
                    .hasPathSatisfying("$.page", page -> assertThat(page).asMap().containsExactlyInAnyOrderEntriesOf(
                            Map.of("size", 7, "number", 2, "totalElements", 60, "totalPages", 9)));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Korean titles arrive intact: book 41 is 진달래꽃")
        void koreanTitle() {
            assertThat(mvc.get().uri("/api/books/41"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.title").isEqualTo("진달래꽃");
        }

        @Test
        @DisplayName("the last book, 60, is there too")
        void lastBook() {
            assertThat(mvc.get().uri("/api/books/60"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.title").isEqualTo("Ultralearning");
        }

        @Test
        @DisplayName("?page=2 answers the last page of 20 books: books 41 to 60")
        void lastPage() {
            assertThat(mvc.get().uri("/api/books?page=2"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content[*].id", ids -> assertThat(ids).asArray().hasSize(20)
                            .startsWith(41).endsWith(60));
        }

        @Test
        @DisplayName("a page after the last one is empty, but still reports the totals")
        void pageAfterTheLast() {
            assertThat(mvc.get().uri("/api/books?page=3"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content", content -> assertThat(content).asArray().isEmpty())
                    .hasPathSatisfying("$.page", page -> assertThat(page).asMap().containsExactlyInAnyOrderEntriesOf(
                            Map.of("size", 20, "number", 3, "totalElements", 60, "totalPages", 3)));
        }

        @Test
        @DisplayName("?size=60 answers every book on one page")
        void everyBookOnOnePage() {
            assertThat(mvc.get().uri("/api/books?size=60"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content", content -> assertThat(content).asArray().hasSize(60))
                    .hasPathSatisfying("$.page.totalPages", pages -> assertThat(pages).isEqualTo(1));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "GET /api/books/{0}")
        @ValueSource(longs = {9999, 61, 0, -1})
        @DisplayName("answers 404 Not Found for an id that no book has")
        void unknownBook(long id) {
            assertThat(mvc.get().uri("/api/books/{id}", id))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("answers 400 Bad Request for an id that isn't a number")
        void idThatIsNotANumber() {
            assertThat(mvc.get().uri("/api/books/abc"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("answers 400 Bad Request for a page number that isn't a number")
        void pageThatIsNotANumber() {
            assertThat(mvc.get().uri("/api/books?page=first"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }
}
