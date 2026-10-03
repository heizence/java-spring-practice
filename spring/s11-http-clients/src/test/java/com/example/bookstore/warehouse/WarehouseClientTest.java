package com.example.bookstore.warehouse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.example.bookstore.support.StubServer;
import com.example.bookstore.support.StubServer.RecordedRequest;
import com.example.bookstore.support.StubServer.StubResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import practice.testkit.Concurrently;

/**
 * {@link WarehouseClient} against a real HTTP server on this computer, which plays the warehouse. The stub server
 * counts the requests, notes when each one arrived, and how many it was working on at the same time.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@DisplayName("S11-03 · Retries and concurrency limits")
class WarehouseClientTest {

    private static final StubServer WAREHOUSE = StubServer.start();

    private static final String ISBN = "9791115881394";
    private static final String STOCK_PATH = "/v1/stock/" + ISBN;
    private static final StubResponse TWELVE_COPIES = StubResponse.json(200, """
            { "isbn": "9791115881394", "available": 12 }""");

    @DynamicPropertySource
    static void pointAtTheStubServer(DynamicPropertyRegistry registry) {
        registry.add("bookstore.warehouse.base-url", WAREHOUSE::baseUrl);
    }

    @Autowired
    private WarehouseClient client;

    @BeforeEach
    void resetTheWarehouse() {
        WAREHOUSE.reset();
    }

    @AfterAll
    static void stopTheWarehouse() {
        WAREHOUSE.close();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("asks GET /v1/stock/{isbn} for JSON, and returns the copies available")
        void returnsTheCopies() {
            WAREHOUSE.respond(STOCK_PATH, TWELVE_COPIES);

            assertThat(client.availableCopies(ISBN)).isEqualTo(12);
            assertThat(WAREHOUSE.requests()).singleElement()
                    .returns("GET", RecordedRequest::method)
                    .returns("application/json", request -> request.header("Accept"));
        }

        @Test
        @DisplayName("a 503 is tried again: 503, 503, 200 returns the copies after 3 requests")
        void retriesA503() {
            WAREHOUSE.respond(STOCK_PATH, StubResponse.status(503), StubResponse.status(503), TWELVE_COPIES);

            assertThat(client.availableCopies(ISBN)).isEqualTo(12);
            assertThat(WAREHOUSE.requests()).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the last of 4 attempts can still succeed: 503, 503, 503, 200")
        void succeedsOnTheLastAttempt() {
            WAREHOUSE.respond(STOCK_PATH,
                    StubResponse.status(503), StubResponse.status(503), StubResponse.status(503), TWELVE_COPIES);

            assertThat(client.availableCopies(ISBN)).isEqualTo(12);
            assertThat(WAREHOUSE.requests()).hasSize(4);
        }

        @Test
        @DisplayName("waits at least 100, then 200, then 400 ms between attempts")
        void backsOff() {
            WAREHOUSE.respond(STOCK_PATH,
                    StubResponse.status(503), StubResponse.status(503), StubResponse.status(503), TWELVE_COPIES);

            client.availableCopies(ISBN);

            assertThat(gapsBetween(WAREHOUSE.requests()))
                    .as("time between attempts")
                    .satisfiesExactly(
                            first -> assertThat(first).isGreaterThanOrEqualTo(Duration.ofMillis(95)),
                            second -> assertThat(second).isGreaterThanOrEqualTo(Duration.ofMillis(195)),
                            third -> assertThat(third).isGreaterThanOrEqualTo(Duration.ofMillis(395)));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a warehouse that answers 503 every time: WarehouseUnavailableException after exactly "
                + "4 requests, within 2 seconds")
        void givesUpAfterFourAttempts() {
            WAREHOUSE.respond(STOCK_PATH, StubResponse.status(503));
            long start = System.nanoTime();

            assertThatThrownBy(() -> client.availableCopies(ISBN)).isInstanceOf(WarehouseUnavailableException.class);

            assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(2));
            assertThat(WAREHOUSE.requests()).hasSize(4);
        }

        @ParameterizedTest(name = "status {0}")
        @ValueSource(ints = {400, 404, 429})
        @DisplayName("a 4xx is not tried again: HttpClientErrorException after one request")
        void doesNotRetryClientErrors(int status) {
            WAREHOUSE.respond(STOCK_PATH, StubResponse.status(status));

            assertThatThrownBy(() -> client.availableCopies(ISBN)).isInstanceOf(HttpClientErrorException.class);

            assertThat(WAREHOUSE.requests()).hasSize(1);
        }

        @Test
        @DisplayName("a 500 is not tried again: HttpServerErrorException after one request")
        void doesNotRetryA500() {
            WAREHOUSE.respond(STOCK_PATH, StubResponse.status(500));

            assertThatThrownBy(() -> client.availableCopies(ISBN)).isInstanceOf(HttpServerErrorException.class);

            assertThat(WAREHOUSE.requests()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("10 calls at the same moment: all succeed, and at most 4 reach the warehouse at the same time")
        void limitsConcurrentCalls() {
            WAREHOUSE.respond(STOCK_PATH, TWELVE_COPIES.delayedBy(Duration.ofMillis(200)));
            Queue<Integer> results = new ConcurrentLinkedQueue<>();

            Concurrently.run(10, i -> results.add(client.availableCopies(ISBN)));

            assertThat(results).hasSize(10).containsOnly(12);
            assertThat(WAREHOUSE.maxConcurrentRequests()).as("requests at the warehouse at the same time")
                    .isEqualTo(WarehouseClient.MAX_CONCURRENT_CALLS);
        }

        @Test
        @DisplayName("calls that are retried keep to the limit too")
        void limitsRetriedCalls() {
            WAREHOUSE.respond(STOCK_PATH, StubResponse.status(503).delayedBy(Duration.ofMillis(50)),
                    StubResponse.status(503).delayedBy(Duration.ofMillis(50)),
                    TWELVE_COPIES.delayedBy(Duration.ofMillis(50)));
            Queue<Integer> results = new ConcurrentLinkedQueue<>();

            Concurrently.run(8, i -> results.add(client.availableCopies(ISBN)));

            assertThat(results).hasSize(8).containsOnly(12);
            assertThat(WAREHOUSE.maxConcurrentRequests()).as("requests at the warehouse at the same time")
                    .isLessThanOrEqualTo(WarehouseClient.MAX_CONCURRENT_CALLS);
        }
    }

    /** The time between the arrival of each request and the next one. */
    private static List<Duration> gapsBetween(List<RecordedRequest> requests) {
        List<Duration> gaps = new ArrayList<>();
        for (int i = 1; i < requests.size(); i++) {
            gaps.add(Duration.ofNanos(requests.get(i).receivedAtNanos() - requests.get(i - 1).receivedAtNanos()));
        }
        return gaps;
    }
}
