package com.example.bookstore.common.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.example.bookstore.order.OrderConfirmationSender;
import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Request ids end to end: the {@code X-Request-Id} header, the MDC, and the console log lines that
 * {@link OutputCaptureExtension} captures. Placing an order writes the line {@code Order 201 placed by member 1: …}.
 */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("S14-01 · Logging with request IDs")
class RequestIdFilterTest {

    private static final String UUID_PATTERN = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private OrderConfirmationSender confirmationSender;

    /** These tests change the test thread's MDC; the next test must not inherit it. */
    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("returns the request's X-Request-Id in the response")
        void returnsTheIncomingId() {
            assertThat(placeOrder("checkout-7f3a")).hasHeader("X-Request-Id", "checkout-7f3a");
        }

        @Test
        @DisplayName("creates a random UUID for a request without X-Request-Id, and returns it")
        void createsAnId() {
            MvcTestResult result = placeOrder(null);

            assertThat(result.getResponse().getHeader("X-Request-Id")).matches(UUID_PATTERN);
        }

        @Test
        @DisplayName("prints the id in the log lines written for the request")
        void printsTheIdInLogLines(CapturedOutput output) {
            placeOrder("checkout-7f3a");

            assertThat(linesContaining(output, "placed by member 1"))
                    .singleElement().asString().contains("checkout-7f3a");
        }

        @Test
        @DisplayName("prints a created id in the log lines too")
        void printsACreatedId(CapturedOutput output) {
            String id = placeOrder(null).getResponse().getHeader("X-Request-Id");

            assertThat(linesContaining(output, "placed by member 1")).singleElement().asString().contains(id);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("removes the id from the MDC when the request is done: the thread serves other requests next")
        void clearsTheMdc() {
            placeOrder("checkout-7f3a");

            assertThat(MDC.get("requestId")).isNull();
        }

        @Test
        @DisplayName("keeps the id when an @Async method logs on another thread")
        void keepsTheIdOnAnotherThread(CapturedOutput output) throws Exception {
            MDC.put("requestId", "checkout-7f3a");
            confirmationSender.send(4242).get(5, TimeUnit.SECONDS);
            MDC.remove("requestId");

            assertThat(linesContaining(output, "Sent the confirmation for order 4242"))
                    .singleElement().asString()
                    .contains("checkout-7f3a")
                    .as("the line was written by a thread of the task executor").contains("task-");
        }

        @Test
        @DisplayName("keeps an id of exactly 64 characters")
        void keeps64Characters() {
            String longest = "a".repeat(64);

            assertThat(placeOrder(longest)).hasHeader("X-Request-Id", longest);
        }

        @Test
        @DisplayName("replaces an id of 65 characters with a new one")
        void replaces65Characters() {
            assertThat(placeOrder("a".repeat(65)).getResponse().getHeader("X-Request-Id")).matches(UUID_PATTERN);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("replaces an id with a line break, which could forge log lines, and logs the new id")
        void replacesAnIdThatCouldForgeLogLines(CapturedOutput output) {
            String forged = "abc\n2026-10-15T00:00:00.000Z  INFO 1 --- [bookstore] Admin password changed";

            MvcTestResult result = placeOrder(forged);

            String id = result.getResponse().getHeader("X-Request-Id");
            assertThat(id).matches(UUID_PATTERN);
            assertThat(output.getOut()).doesNotContain("Admin password changed");
            assertThat(linesContaining(output, "placed by member 1")).singleElement().asString().contains(id);
        }

        @Test
        @DisplayName("a failed request also returns its id, and leaves the MDC empty")
        void failedRequestKeepsTheId() {
            MvcTestResult result = mvc.post().uri("/api/orders").header("X-Request-Id", "checkout-7f3a")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "memberId": 1, "lines": [ { "bookId": 59, "quantity": 1 } ] }""")
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CONFLICT).hasHeader("X-Request-Id", "checkout-7f3a");
            assertThat(MDC.get("requestId")).isNull();
        }
    }

    /** Places an order for book 1 as member 1, with the header if {@code requestId} isn't {@code null}. */
    private MvcTestResult placeOrder(String requestId) {
        var request = mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "memberId": 1, "lines": [ { "bookId": 1, "quantity": 1 } ] }""");
        if (requestId != null) {
            request.header("X-Request-Id", requestId);
        }
        MvcTestResult result = request.exchange();
        assertThat(result).as("POST /api/orders").hasStatus(HttpStatus.CREATED);
        return result;
    }

    private static List<String> linesContaining(CapturedOutput output, String text) {
        return output.getOut().lines().filter(line -> line.contains(text)).toList();
    }
}
