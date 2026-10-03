package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Two versions of {@code GET /api/books/{id}}, chosen by the {@code API-Version} request header. */
@IntegrationTest
@DisplayName("S04-08 · API versioning")
class ApiVersioningApiTest {

    private static final String API_VERSION = "API-Version";

    private static final String VERSION_1 = """
            {
              "id": 1,
              "isbn": "9791115881394",
              "title": "Effective Java",
              "author": "Joshua Bloch",
              "category": "Programming",
              "price": 36000,
              "publishedDate": "2018-10-18"
            }""";

    private static final String VERSION_2 = """
            {
              "id": 1,
              "isbn": "9791115881394",
              "title": "Effective Java",
              "author": "Joshua Bloch",
              "category": { "id": 1, "name": "Programming" },
              "price": { "amount": 36000, "currency": "KRW" },
              "publishedDate": "2018-10-18"
            }""";

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("API-Version: 2 answers version 2: category and price as objects")
        void version2() {
            assertThat(mvc.get().uri("/api/books/1").header(API_VERSION, "2"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo(VERSION_2);
        }

        @Test
        @DisplayName("a request without API-Version gets version 1, so existing clients keep working")
        void withoutVersion() {
            assertThat(mvc.get().uri("/api/books/1"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo(VERSION_1);
        }

        @Test
        @DisplayName("API-Version: 1 answers version 1")
        void version1() {
            assertThat(mvc.get().uri("/api/books/1").header(API_VERSION, "1"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo(VERSION_1);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @ParameterizedTest(name = "API-Version: {0}")
        @ValueSource(strings = {"2.0", "v2"})
        @DisplayName("versions are numbers, optionally after a v: 2.0 and v2 are version 2")
        void versionAsNumber(String version) {
            assertThat(mvc.get().uri("/api/books/1").header(API_VERSION, version))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo(VERSION_2);
        }

        @Test
        @DisplayName("endpoints without a version 2 answer as before, also to API-Version: 2")
        void unversionedEndpoint() {
            assertThat(mvc.get().uri("/api/books").header(API_VERSION, "2"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.content[0].category").isEqualTo("Programming");
        }

        @Test
        @DisplayName("version 2 of a book that doesn't exist: 404 with code BOOK_NOT_FOUND")
        void unknownBookInVersion2() {
            assertThat(mvc.get().uri("/api/books/9999").header(API_VERSION, "2"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("BOOK_NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "API-Version: {0}")
        @ValueSource(strings = {"3", "0", "abc", "2a"})
        @DisplayName("a version that the API doesn't have: 400 with code INVALID_INPUT")
        void unsupportedVersion(String version) {
            assertThat(mvc.get().uri("/api/books/1").header(API_VERSION, version))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .hasPathSatisfying("$.title", title -> assertThat(title).isEqualTo("Bad Request"))
                    .hasPathSatisfying("$.instance", instance -> assertThat(instance).isEqualTo("/api/books/1"))
                    .hasPathSatisfying("$.code", code -> assertThat(code).isEqualTo("INVALID_INPUT"))
                    .hasPathSatisfying("$.detail", detail -> assertThat(detail).asString().contains("API version"));
        }

        @Test
        @DisplayName("the version is checked for every endpoint: GET /api/books with API-Version: 3 is 400 too")
        void unsupportedVersionElsewhere() {
            assertThat(mvc.get().uri("/api/books").header(API_VERSION, "3"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }
}
