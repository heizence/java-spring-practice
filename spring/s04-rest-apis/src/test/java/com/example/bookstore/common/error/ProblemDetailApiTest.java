package com.example.bookstore.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Every kind of error becomes exactly the problem-detail JSON of {@code spring/BOOKSTORE.md}. */
@IntegrationTest
@DisplayName("S04-03 · Error responses with ProblemDetail")
class ProblemDetailApiTest {

    private static final String VALID_BOOK = """
            {
              "isbn": "9788966261208",
              "title": "Effective Kotlin",
              "author": "Marcin Moskala",
              "categoryId": 1,
              "price": 32000,
              "publishedDate": "2022-03-07"
            }""";

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a book that doesn't exist: 404 with code BOOK_NOT_FOUND")
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
        @DisplayName("a broken business rule, a second book with the same ISBN: 409 with code DUPLICATE_ISBN")
        void duplicateIsbn() {
            String bookOneIsbn = VALID_BOOK.replace("9788966261208", "9791115881394");

            assertThat(post(bookOneIsbn))
                    .hasStatus(HttpStatus.CONFLICT)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Conflict",
                              "status": 409,
                              "detail": "A book with ISBN 9791115881394 already exists.",
                              "instance": "/api/books",
                              "code": "DUPLICATE_ISBN"
                            }""");
        }

        @Test
        @DisplayName("invalid input: 400 with code INVALID_INPUT and every invalid field in errors, sorted by field")
        void invalidBody() {
            String book = """
                    { "isbn": "97911", "title": " ", "author": "Marcin Moskala", "categoryId": 1, "price": 0,
                      "publishedDate": "2022-03-07" }""";

            assertThat(post(book))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Invalid request content.",
                              "instance": "/api/books",
                              "code": "INVALID_INPUT",
                              "errors": [
                                { "field": "isbn", "message": "must be 13 digits" },
                                { "field": "price", "message": "must be greater than 0" },
                                { "field": "title", "message": "must not be blank" }
                              ]
                            }""");
        }

        @Test
        @DisplayName("malformed JSON: 400 with code INVALID_INPUT")
        void malformedJson() {
            assertThat(post("{ \"isbn\": "))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Failed to read request",
                              "instance": "/api/books",
                              "code": "INVALID_INPUT"
                            }""");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a field that breaks two rules is listed twice, sorted by message")
        void twoErrorsForOneField() {
            String book = VALID_BOOK.replace("\"Effective Kotlin\"", "\"" + " ".repeat(201) + "\"");

            assertThat(post(book))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.errors").isEqualTo(List.of(
                            Map.of("field", "title", "message", "must not be blank"),
                            Map.of("field", "title", "message", "size must be between 0 and 200")));
        }

        @Test
        @DisplayName("validation messages are in English, even when the client asks for Korean")
        void englishMessages() {
            String book = VALID_BOOK.replace("\"price\": 32000", "\"price\": 0");

            assertThat(mvc.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(book)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.errors[0].message").isEqualTo("must be greater than 0");
        }

        @Test
        @DisplayName("a category that doesn't exist: 404 with code CATEGORY_NOT_FOUND")
        void unknownCategory() {
            assertThat(post(VALID_BOOK.replace("\"categoryId\": 1", "\"categoryId\": 99")))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Category 99 was not found.",
                              "instance": "/api/books",
                              "code": "CATEGORY_NOT_FOUND"
                            }""");
        }

        @Test
        @DisplayName("an id that isn't a number: 400 with code INVALID_INPUT")
        void idThatIsNotANumber() {
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

    @Nested
    @DisplayName("Errors that Spring MVC detects")
    class SpringMvcErrors {

        @Test
        @DisplayName("an unknown path: 404 with code NOT_FOUND")
        void unknownPath() {
            assertThat(mvc.get().uri("/api/bookz"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "No endpoint GET /api/bookz.",
                              "instance": "/api/bookz",
                              "code": "NOT_FOUND"
                            }""");
        }

        @Test
        @DisplayName("an HTTP method the path doesn't support: 405 with code METHOD_NOT_ALLOWED")
        void wrongMethod() {
            assertThat(mvc.delete().uri("/api/books"))
                    .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Method Not Allowed",
                              "status": 405,
                              "detail": "Method 'DELETE' is not supported.",
                              "instance": "/api/books",
                              "code": "METHOD_NOT_ALLOWED"
                            }""");
        }

        @Test
        @DisplayName("a body that isn't JSON: 415 with code UNSUPPORTED_MEDIA_TYPE")
        void wrongContentType() {
            assertThat(mvc.post().uri("/api/books").contentType(MediaType.TEXT_PLAIN).content("Effective Kotlin"))
                    .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .bodyJson()
                    .hasPathSatisfying("$.status", status -> assertThat(status).isEqualTo(415))
                    .hasPathSatisfying("$.code", code -> assertThat(code).isEqualTo("UNSUPPORTED_MEDIA_TYPE"));
        }
    }

    private MvcTestResult post(String json) {
        return mvc.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }
}
