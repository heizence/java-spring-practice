package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * {@code PUT}, {@code PATCH} and {@code DELETE /api/books/{id}}, with optimistic concurrency through {@code ETag} and
 * {@code If-Match}. Every seeded book starts at version 0, so its ETag is {@code "0"}.
 */
@IntegrationTest
@DisplayName("S04-07 · Updating and deleting resources")
class BookUpdateApiTest {

    private static final String REPLACEMENT = """
            {
              "title": "Effective Java, Third Edition",
              "author": "Joshua Bloch",
              "categoryId": 3,
              "price": 38000,
              "publishedDate": "2018-11-01"
            }""";

    private static final String BOOK_ONE = """
            {
              "id": 1,
              "isbn": "9791115881394",
              "title": "Effective Java",
              "author": "Joshua Bloch",
              "category": "Programming",
              "price": 36000,
              "publishedDate": "2018-10-18"
            }""";

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("GET /api/books/1 sends the book's version as its ETag: \"0\"")
        void eTagOfABook() {
            assertThat(mvc.get().uri("/api/books/1"))
                    .hasStatusOk()
                    .hasHeader(HttpHeaders.ETAG, "\"0\"")
                    .bodyJson().isStrictlyEqualTo(BOOK_ONE);
        }

        @Test
        @DisplayName("PUT replaces every field but the ISBN, and answers 200 with the book and its new ETag")
        void replacesTheBook() {
            assertThat(put(1, "\"0\"", REPLACEMENT))
                    .hasStatusOk()
                    .hasHeader(HttpHeaders.ETAG, "\"1\"")
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "id": 1,
                              "isbn": "9791115881394",
                              "title": "Effective Java, Third Edition",
                              "author": "Joshua Bloch",
                              "category": "Software Architecture",
                              "price": 38000,
                              "publishedDate": "2018-11-01"
                            }""");
        }

        @Test
        @DisplayName("after a PUT, GET shows the new book and its new ETag")
        void putIsStored() {
            put(1, "\"0\"", REPLACEMENT);

            assertThat(mvc.get().uri("/api/books/1"))
                    .hasHeader(HttpHeaders.ETAG, "\"1\"")
                    .bodyJson().extractingPath("$.title").isEqualTo("Effective Java, Third Edition");
        }

        @Test
        @DisplayName("PATCH changes only the fields it contains, and answers 200 with the book and its new ETag")
        void changesSomeFields() {
            assertThat(patch(1, "\"0\"", "{ \"price\": 32000 }"))
                    .hasStatusOk()
                    .hasHeader(HttpHeaders.ETAG, "\"1\"")
                    .bodyJson().isStrictlyEqualTo(BOOK_ONE.replace("36000", "32000"));
        }

        @Test
        @DisplayName("DELETE answers 204 No Content, and the book is gone")
        void deletesTheBook() {
            assertThat(mvc.delete().uri("/api/books/1"))
                    .hasStatus(HttpStatus.NO_CONTENT)
                    .body().isEmpty();
            assertThat(mvc.get().uri("/api/books/1")).hasStatus(HttpStatus.NOT_FOUND);
        }

        @Test
        @DisplayName("a PUT with an old ETag answers 412 Precondition Failed and changes nothing")
        void staleETag() {
            put(1, "\"0\"", REPLACEMENT);

            assertThat(put(1, "\"0\"", REPLACEMENT.replace("38000", "1000")))
                    .hasStatus(HttpStatus.PRECONDITION_FAILED)
                    .bodyJson()
                    .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo(412))
                    .hasPathSatisfying("$.code", code -> assertThat(code).isEqualTo("PRECONDITION_FAILED"));
            assertThat(mvc.get().uri("/api/books/1"))
                    .hasHeader(HttpHeaders.ETAG, "\"1\"")
                    .bodyJson().extractingPath("$.price").isEqualTo(38000);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("each change gets the next version, and the client must send the newest ETag")
        void versionsCount() {
            assertThat(patch(1, "\"0\"", "{ \"price\": 32000 }")).hasHeader(HttpHeaders.ETAG, "\"1\"");
            assertThat(patch(1, "\"1\"", "{ \"price\": 30000 }")).hasHeader(HttpHeaders.ETAG, "\"2\"");

            assertThat(patch(1, "\"1\"", "{ \"price\": 1000 }")).hasStatus(HttpStatus.PRECONDITION_FAILED);
        }

        @Test
        @DisplayName("an empty PATCH changes nothing and keeps the ETag")
        void emptyPatch() {
            assertThat(patch(1, "\"0\"", "{}"))
                    .hasStatusOk()
                    .hasHeader(HttpHeaders.ETAG, "\"0\"")
                    .bodyJson().isStrictlyEqualTo(BOOK_ONE);
        }

        @Test
        @DisplayName("a PUT with the values the book already has keeps the ETag")
        void putWithoutChanges() {
            String sameValues = """
                    { "title": "Effective Java", "author": "Joshua Bloch", "categoryId": 1, "price": 36000,
                      "publishedDate": "2018-10-18" }""";

            assertThat(put(1, "\"0\"", sameValues))
                    .hasStatusOk()
                    .hasHeader(HttpHeaders.ETAG, "\"0\"");
        }

        @Test
        @DisplayName("PATCH can change the category, and the book shows the new category's name")
        void patchCategory() {
            assertThat(patch(1, "\"0\"", "{ \"categoryId\": 4 }"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.category").isEqualTo("Fiction");
        }

        @Test
        @DisplayName("PUT ignores an isbn in the body: the ISBN never changes")
        void isbnNeverChanges() {
            String withIsbn = REPLACEMENT.replace("{", "{ \"isbn\": \"9788966261208\",");

            assertThat(put(1, "\"0\"", withIsbn))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.isbn").isEqualTo("9791115881394");
        }

        @Test
        @DisplayName("a repeated DELETE answers 204 too: the book is gone either way")
        void repeatedDelete() {
            assertThat(mvc.delete().uri("/api/books/1")).hasStatus(HttpStatus.NO_CONTENT);

            assertThat(mvc.delete().uri("/api/books/1")).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(mvc.delete().uri("/api/books/9999")).hasStatus(HttpStatus.NO_CONTENT);
        }

        @Test
        @DisplayName("a deleted book leaves the list")
        void deletedBookLeavesTheList() {
            mvc.delete().uri("/api/books/1").exchange();

            assertThat(mvc.get().uri("/api/books"))
                    .bodyJson()
                    .hasPathSatisfying("$.page.totalElements", total -> assertThat(total).isEqualTo(59))
                    .hasPathSatisfying("$.content[0].id", id -> assertThat(id).isEqualTo(2));
        }

        @Test
        @DisplayName("a GET with If-None-Match and the current ETag answers 304 Not Modified, without a body")
        void notModified() {
            assertThat(mvc.get().uri("/api/books/1").header(HttpHeaders.IF_NONE_MATCH, "\"0\""))
                    .hasStatus(HttpStatus.NOT_MODIFIED)
                    .body().isEmpty();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a PUT without If-Match answers 428 Precondition Required")
        void putWithoutIfMatch() {
            assertThat(mvc.put().uri("/api/books/1").contentType(MediaType.APPLICATION_JSON).content(REPLACEMENT))
                    .hasStatus(HttpStatus.PRECONDITION_REQUIRED)
                    .bodyJson()
                    .hasPathSatisfying("$.title", title -> assertThat(title).isEqualTo("Precondition Required"))
                    .hasPathSatisfying("$.code", code -> assertThat(code).isEqualTo("PRECONDITION_REQUIRED"))
                    .hasPathSatisfying("$.instance", instance -> assertThat(instance).isEqualTo("/api/books/1"));
        }

        @Test
        @DisplayName("a PATCH without If-Match answers 428 too, and changes nothing")
        void patchWithoutIfMatch() {
            assertThat(mvc.patch().uri("/api/books/1").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"price\": 1000 }"))
                    .hasStatus(HttpStatus.PRECONDITION_REQUIRED);
            assertThat(mvc.get().uri("/api/books/1")).bodyJson().extractingPath("$.price").isEqualTo(36000);
        }

        @ParameterizedTest(name = "If-Match: {0}")
        @ValueSource(strings = {"W/\"0\"", "0", "\"abc\"", "\"1\"", "\"-1\""})
        @DisplayName("an If-Match that isn't the book's current ETag answers 412")
        void notTheCurrentETag(String ifMatch) {
            assertThat(put(1, ifMatch, REPLACEMENT))
                    .hasStatus(HttpStatus.PRECONDITION_FAILED)
                    .bodyJson().extractingPath("$.code").isEqualTo("PRECONDITION_FAILED");
        }

        @Test
        @DisplayName("a PATCH with an old ETag answers 412")
        void patchWithStaleETag() {
            patch(1, "\"0\"", "{ \"price\": 32000 }");

            assertThat(patch(1, "\"0\"", "{ \"title\": \"Effective Java 2\" }"))
                    .hasStatus(HttpStatus.PRECONDITION_FAILED);
        }

        @Test
        @DisplayName("a PUT with an invalid body answers 400 and lists the errors, like POST")
        void invalidPut() {
            String invalid = """
                    { "title": " ", "author": "Joshua Bloch", "categoryId": 1, "price": 0, "publishedDate": null }""";

            assertThat(put(1, "\"0\"", invalid))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.errors").isEqualTo(List.of(
                            Map.of("field", "price", "message", "must be greater than 0"),
                            Map.of("field", "publishedDate", "message", "must not be null"),
                            Map.of("field", "title", "message", "must not be blank")));
        }

        @Test
        @DisplayName("a PATCH validates the fields it contains: a blank title, a long author, a price of 0")
        void invalidPatch() {
            String invalid = """
                    { "title": "  ", "author": "%s", "price": 0 }""".formatted("a".repeat(101));

            assertThat(patch(1, "\"0\"", invalid))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.errors").isEqualTo(List.of(
                            Map.of("field", "author", "message", "size must be between 0 and 100"),
                            Map.of("field", "price", "message", "must be greater than 0"),
                            Map.of("field", "title", "message", "must not be blank")));
        }

        @Test
        @DisplayName("PUT and PATCH on a book that doesn't exist answer 404 with code BOOK_NOT_FOUND")
        void unknownBook() {
            assertThat(put(9999, "\"0\"", REPLACEMENT))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("BOOK_NOT_FOUND");
            assertThat(patch(9999, "\"0\"", "{}"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("BOOK_NOT_FOUND");
        }

        @Test
        @DisplayName("a PUT with a category that doesn't exist answers 404 with code CATEGORY_NOT_FOUND")
        void unknownCategory() {
            assertThat(put(1, "\"0\"", REPLACEMENT.replace("\"categoryId\": 3", "\"categoryId\": 99")))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("CATEGORY_NOT_FOUND");
        }
    }

    private MvcTestResult put(long id, String ifMatch, String json) {
        return mvc.put().uri("/api/books/{id}", id).header(HttpHeaders.IF_MATCH, ifMatch)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private MvcTestResult patch(long id, String ifMatch, String json) {
        return mvc.patch().uri("/api/books/{id}", id).header(HttpHeaders.IF_MATCH, ifMatch)
                .contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }
}
