package com.example.bookstore.publisher;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.example.bookstore.support.StubServer;
import com.example.bookstore.support.StubServer.RecordedRequest;
import com.example.bookstore.support.StubServer.StubResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

/**
 * {@link PublisherCatalogClient} against a real HTTP server on this computer, which plays the publishers' catalog.
 * The application starts without a web server; the test points the client's base URL at the stub server and sets
 * the API key's environment variable.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("S11-02 · HTTP interface clients")
class PublisherCatalogClientTest {

    private static final StubServer CATALOG = StubServer.start();

    private static final String EFFECTIVE_JAVA = """
            { "isbn": "9791115881394", "title": "Effective Java", "publisher": "Insight",
              "publishedDate": "2018-10-18", "listPrice": 36000 }""";

    @DynamicPropertySource
    static void pointAtTheStubServer(DynamicPropertyRegistry registry) {
        registry.add("spring.http.serviceclient.publisher-catalog.base-url", CATALOG::baseUrl);
        registry.add("PUBLISHER_CATALOG_API_KEY", () -> "test-api-key");
    }

    @Autowired
    private PublisherCatalogClient client;

    @Autowired
    private Environment environment;

    @BeforeEach
    void resetTheCatalog() {
        CATALOG.reset();
    }

    @AfterAll
    static void stopTheCatalog() {
        CATALOG.close();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("findBook asks GET /v1/books/{isbn} and maps the JSON to a PublisherBook")
        void findsABook() {
            CATALOG.respond("/v1/books/9791115881394", StubResponse.json(200, EFFECTIVE_JAVA));

            PublisherBook book = client.findBook("9791115881394");

            assertThat(book).isEqualTo(new PublisherBook("9791115881394", "Effective Java", "Insight",
                    LocalDate.of(2018, 10, 18), 36_000));
            assertThat(CATALOG.requests()).singleElement().returns("GET", RecordedRequest::method);
        }

        @Test
        @DisplayName("findBooksByAuthor sends the author and the limit as query parameters")
        void findsBooksByAuthor() {
            CATALOG.respond("/v1/books", StubResponse.json(200, "[" + EFFECTIVE_JAVA + "]"));

            List<PublisherBook> books = client.findBooksByAuthor("Joshua Bloch", 5);

            assertThat(books).extracting(PublisherBook::title).containsExactly("Effective Java");
            assertThat(CATALOG.requests()).singleElement()
                    .returns("author=Joshua Bloch&limit=5", RecordedRequest::query);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("every request carries the API key from the environment variable, and asks for JSON")
        void sendsTheApiKey() {
            CATALOG.respond("/v1/books/9791115881394", StubResponse.json(200, EFFECTIVE_JAVA));

            client.findBook("9791115881394");

            RecordedRequest request = CATALOG.requests().getFirst();
            assertThat(request.header("X-Api-Key")).isEqualTo("test-api-key");
            assertThat(request.header("Accept")).isEqualTo("application/json");
        }

        @Test
        @DisplayName("an author without books gives an empty list")
        void noBooks() {
            CATALOG.respond("/v1/books", StubResponse.json(200, "[]"));

            assertThat(client.findBooksByAuthor("Nobody", 5)).isEmpty();
        }

        @Test
        @DisplayName("the catalog's client connects with a timeout of 1 second, set in application.yml")
        void connectTimeout() {
            assertThat(environment.getProperty("spring.http.serviceclient.publisher-catalog.connect-timeout",
                    Duration.class)).isEqualTo(Duration.ofSeconds(1));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an ISBN that the catalog doesn't know fails with HttpClientErrorException.NotFound")
        void unknownIsbn() {
            CATALOG.respond("/v1/books/9790000000000", StubResponse.status(404));

            assertThatThrownBy(() -> client.findBook("9790000000000"))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
        }

        @Test
        @DisplayName("a catalog that doesn't answer fails after the read timeout of 2 seconds, "
                + "with a ResourceAccessException")
        void slowCatalog() {
            CATALOG.respond("/v1/books/9791115881394",
                    StubResponse.json(200, EFFECTIVE_JAVA).delayedBy(Duration.ofSeconds(8)));
            long start = System.nanoTime();

            assertThatThrownBy(() -> client.findBook("9791115881394"))
                    .isInstanceOf(ResourceAccessException.class);

            Duration waited = Duration.ofNanos(System.nanoTime() - start);
            assertThat(waited).as("time until the client gave up")
                    .isGreaterThanOrEqualTo(Duration.ofMillis(1_800))
                    .isLessThan(Duration.ofMillis(4_500));
        }
    }
}
