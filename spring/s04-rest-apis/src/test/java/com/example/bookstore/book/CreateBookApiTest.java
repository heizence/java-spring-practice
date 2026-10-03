package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import com.example.bookstore.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** {@code POST /api/books} over HTTP. {@link CreateBookRequestTest} checks every validation rule on its own. */
@IntegrationTest
@DisplayName("S04-02 · Request bodies and validation")
class CreateBookApiTest {

    private static final String NEW_BOOK = """
            {
              "isbn": "9788966261208",
              "title": "Effective Kotlin",
              "author": "Marcin Moskala",
              "categoryId": 1,
              "price": 32000,
              "publishedDate": "2022-03-07"
            }""";

    /** The new book as the API shows it, without its id, which the server chooses. */
    private static final String NEW_BOOK_RESPONSE = """
            {
              "isbn": "9788966261208",
              "title": "Effective Kotlin",
              "author": "Marcin Moskala",
              "category": "Programming",
              "price": 32000,
              "publishedDate": "2022-03-07"
            }""";

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("answers 201 Created with the new book as JSON")
        void createsABook() {
            assertThat(post(NEW_BOOK))
                    .hasStatus(HttpStatus.CREATED)
                    .hasContentType(MediaType.APPLICATION_JSON)
                    .bodyJson()
                    .isLenientlyEqualTo(NEW_BOOK_RESPONSE)
                    .hasPathSatisfying("$.id", id -> assertThat(id).asNumber().isNotNull());
        }

        @Test
        @DisplayName("the Location header is the new book's address, /api/books/{id}")
        void locationHeader() {
            MvcTestResult result = post(NEW_BOOK);

            assertThat(result).hasStatus(HttpStatus.CREATED).containsHeader(HttpHeaders.LOCATION);
            assertThat(URI.create(locationOf(result)).getPath()).isEqualTo("/api/books/" + idOf(result));
        }

        @Test
        @DisplayName("the new book can be read at its Location")
        void newBookCanBeRead() {
            MvcTestResult created = post(NEW_BOOK);

            assertThat(mvc.get().uri(URI.create(locationOf(created)).getPath()))
                    .hasStatusOk()
                    .bodyJson().isLenientlyEqualTo(NEW_BOOK_RESPONSE);
        }

        @Test
        @DisplayName("the catalog has one book more")
        void catalogGrows() {
            post(NEW_BOOK);

            assertThat(mvc.get().uri("/api/books"))
                    .bodyJson().extractingPath("$.page.totalElements").isEqualTo(61);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("each new book gets its own id")
        void newIds() {
            long first = idOf(post(NEW_BOOK));
            long second = idOf(post(NEW_BOOK.replace("9788966261208", "9788966261215")));

            assertThat(second).isNotEqualTo(first);
            assertThat(first).isGreaterThan(60);
        }

        @Test
        @DisplayName("Korean text arrives intact")
        void koreanText() {
            String book = NEW_BOOK.replace("Effective Kotlin", "채식주의자").replace("Marcin Moskala", "한강");

            assertThat(post(book))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .hasPathSatisfying("$.title", title -> assertThat(title).isEqualTo("채식주의자"))
                    .hasPathSatisfying("$.author", author -> assertThat(author).isEqualTo("한강"));
        }

        @Test
        @DisplayName("spaces around the title and author are removed")
        void stripsSpaces() {
            String book = NEW_BOOK.replace("\"Effective Kotlin\"", "\"  Effective Kotlin  \"");

            assertThat(post(book))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson().extractingPath("$.title").isEqualTo("Effective Kotlin");
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("answers 400 Bad Request for a book without a title, and stores nothing")
        void invalidBook() {
            String book = NEW_BOOK.replace("\"Effective Kotlin\"", "\"  \"");

            assertThat(post(book)).hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(mvc.get().uri("/api/books"))
                    .bodyJson().extractingPath("$.page.totalElements").isEqualTo(60);
        }

        @Test
        @DisplayName("answers 400 Bad Request for an ISBN with hyphens")
        void isbnWithHyphens() {
            assertThat(post(NEW_BOOK.replace("9788966261208", "978-89-6626-120-8")))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("answers 400 Bad Request for a book without a price")
        void missingPrice() {
            assertThat(post(NEW_BOOK.replace("\"price\": 32000,", "")))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("answers 400 Bad Request for a request without a body")
        void noBody() {
            assertThat(mvc.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("answers 400 Bad Request for malformed JSON")
        void malformedJson() {
            assertThat(post("{ \"isbn\": "))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("answers 415 Unsupported Media Type for a body that isn't JSON")
        void notJson() {
            assertThat(mvc.post().uri("/api/books").contentType(MediaType.TEXT_PLAIN).content("Effective Kotlin"))
                    .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
        }
    }

    private MvcTestResult post(String json) {
        return mvc.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }

    private static String locationOf(MvcTestResult result) {
        String location = result.getResponse().getHeader(HttpHeaders.LOCATION);
        assertThat(location).as("Location header").isNotBlank();
        return location;
    }

    private static long idOf(MvcTestResult result) {
        assertThat(result).hasStatus(HttpStatus.CREATED);
        try {
            Number id = JsonPath.read(result.getResponse().getContentAsString(StandardCharsets.UTF_8), "$.id");
            return id.longValue();
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
    }
}
