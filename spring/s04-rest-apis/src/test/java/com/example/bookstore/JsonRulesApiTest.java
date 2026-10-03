package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import tools.jackson.databind.json.JsonMapper;

/**
 * The JSON rules of {@code spring/BOOKSTORE.md}, for every endpoint: through {@code GET /api/books/{id}/availability}
 * and {@code POST /api/books}, and through the application's own {@link JsonMapper}, which writes every response.
 */
@IntegrationTest
@DisplayName("S04-04 · JSON with Jackson")
class JsonRulesApiTest {

    private static final String NEW_BOOK = """
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

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private TestClock clock;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("times are ISO-8601 strings in UTC, dates are ISO dates, enums are names")
        void availableBook() {
            // The clock stands at 2026-10-15T00:00:00Z: 09:00 in Seoul, before the 14:00 cutoff.
            assertThat(mvc.get().uri("/api/books/1/availability"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "bookId": 1,
                              "status": "IN_STOCK",
                              "quantity": 10,
                              "orderBy": "2026-10-15T05:00:00Z",
                              "estimatedDelivery": "2026-10-16",
                              "checkedAt": "2026-10-15T00:00:00Z"
                            }""");
        }

        @Test
        @DisplayName("properties whose value is null are left out")
        void outOfStockBook() {
            assertThat(mvc.get().uri("/api/books/59/availability"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "bookId": 59,
                              "status": "OUT_OF_STOCK",
                              "quantity": 0,
                              "checkedAt": "2026-10-15T00:00:00Z"
                            }""");
        }

        @Test
        @DisplayName("a price with a fraction is rejected with 400 instead of being cut to whole won")
        void fractionalPrice() {
            assertThat(post(NEW_BOOK.replace("\"price\": 32000", "\"price\": 32000.5")))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT");
        }

        @Test
        @DisplayName("properties that a request doesn't know are ignored")
        void unknownProperty() {
            String book = NEW_BOOK.replace("\"title\":", "\"subtitle\": \"Best practices\", \"title\":");

            assertThat(post(book)).hasStatus(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("from 14:00 in Seoul, the book ships the next day: orderBy is tomorrow, still in UTC")
        void afterTheCutoff() {
            clock.setInstant(Instant.parse("2026-10-15T05:00:00Z")); // 14:00 in Seoul

            assertThat(mvc.get().uri("/api/books/1/availability"))
                    .bodyJson()
                    .hasPathSatisfying("$.orderBy", orderBy -> assertThat(orderBy).isEqualTo("2026-10-16T05:00:00Z"))
                    .hasPathSatisfying("$.estimatedDelivery",
                            delivery -> assertThat(delivery).isEqualTo("2026-10-17"))
                    .hasPathSatisfying("$.checkedAt",
                            checkedAt -> assertThat(checkedAt).isEqualTo("2026-10-15T05:00:00Z"));
        }

        @Test
        @DisplayName("after midnight in Seoul, the dates follow Seoul's calendar, not UTC's")
        void afterMidnightInSeoul() {
            clock.setInstant(Instant.parse("2026-10-15T16:00:00Z")); // 01:00 on October 16 in Seoul

            assertThat(mvc.get().uri("/api/books/1/availability"))
                    .bodyJson()
                    .hasPathSatisfying("$.orderBy", orderBy -> assertThat(orderBy).isEqualTo("2026-10-16T05:00:00Z"))
                    .hasPathSatisfying("$.estimatedDelivery",
                            delivery -> assertThat(delivery).isEqualTo("2026-10-17"));
        }

        @Test
        @DisplayName("an enum value is written by its name: LOW_STOCK")
        void lowStock() {
            inventoryRepository.save(new Inventory(1, 3));

            assertThat(mvc.get().uri("/api/books/1/availability"))
                    .bodyJson().extractingPath("$.status").isEqualTo("LOW_STOCK");
        }

        @Test
        @DisplayName("a price written with a decimal point is rejected too: 32000.0 isn't a whole number in JSON")
        void decimalPointPrice() {
            assertThat(post(NEW_BOOK.replace("\"price\": 32000", "\"price\": 32000.0")))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    @Nested
    @DisplayName("Every response")
    class EveryResponse {

        @Test
        @DisplayName("the application's JsonMapper writes every zoned time in UTC")
        void zonedTimes() {
            Instant instant = Instant.parse("2026-10-15T00:00:00Z");
            var times = new Times(instant.atZone(ZoneId.of("Asia/Seoul")), instant.atOffset(ZoneOffset.ofHours(-5)),
                    instant, LocalDate.of(2026, 10, 15));

            assertThat(jsonMapper.writeValueAsString(times)).isEqualTo("""
                    {"zoned":"2026-10-15T00:00:00Z","offset":"2026-10-15T00:00:00Z",\
                    "instant":"2026-10-15T00:00:00Z","date":"2026-10-15"}""");
        }

        @Test
        @DisplayName("the application's JsonMapper leaves out every null property")
        void nulls() {
            assertThat(jsonMapper.writeValueAsString(new Times(null, null, Instant.EPOCH, null)))
                    .isEqualTo("{\"instant\":\"1970-01-01T00:00:00Z\"}");
        }
    }

    /** A response with every kind of time, to check the application's JSON settings directly. */
    record Times(ZonedDateTime zoned, OffsetDateTime offset, Instant instant, LocalDate date) {
    }

    private MvcTestResult post(String json) {
        return mvc.post().uri("/api/books").contentType(MediaType.APPLICATION_JSON).content(json).exchange();
    }
}
