package com.example.bookstore.reservation;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.Responses;
import com.example.bookstore.support.SeedData;
import com.example.bookstore.support.TestClock;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Store pickup reservations: {@code POST /api/books/{bookId}/reservations}. Book 1 has 10 copies in stock, books 59
 * and 60 have none; members 1 to 20 exist ({@code spring/BOOKSTORE.md}). A member may hold 2 copies of a book.
 *
 * <p>Every request commits, as in production, and each test puts the seed data back afterwards.
 */
@IntegrationTest
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@DisplayName("S08-03 · Pessimistic locking")
class ReservationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private SeedData seedData;

    @AfterEach
    void restoreSeedData() {
        seedData.restore();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("a reservation takes its copies out of stock and answers 201 with the reservation")
        void reserves() {
            MvcTestResult result = reserve(1, 1, 2);

            assertThat(result)
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson().isLenientlyEqualTo("""
                            {
                              "bookId": 1,
                              "memberId": 1,
                              "quantity": 2,
                              "reservedAt": "%s"
                            }""".formatted(TestClock.DEFAULT_INSTANT));
            Number id = JsonPath.read(Responses.body(result), "$.id");
            assertThat(result).headers().hasValue(HttpHeaders.LOCATION, "/api/reservations/" + id);
            assertThat(stockOf(1)).isEqualTo(8);
        }

        @Test
        @DisplayName("the new reservation can be read at its Location")
        void canBeReadBack() {
            String location = reserve(1, 3, 1).getResponse().getHeader(HttpHeaders.LOCATION);

            assertThat(mvc.get().uri(location))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.memberId").asNumber().isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a member can reserve one copy, then a second one, up to the limit of 2")
        void reservesUpToTheLimit() {
            MvcTestResult first = reserve(1, 1, 1);
            MvcTestResult second = reserve(1, 1, 1);

            assertThat(first).hasStatus(HttpStatus.CREATED);
            assertThat(second).hasStatus(HttpStatus.CREATED);
            assertThat(stockOf(1)).isEqualTo(8);
        }

        @Test
        @DisplayName("the limit counts per book: a member can hold 2 copies of each of two books")
        void limitIsPerBook() {
            int book2Before = stockOf(2);

            reserve(1, 1, 2);
            MvcTestResult otherBook = reserve(2, 1, 1);

            assertThat(otherBook).hasStatus(HttpStatus.CREATED);
            assertThat(stockOf(2)).isEqualTo(book2Before - 1);
        }

        @Test
        @DisplayName("five members can reserve all 10 copies of a book, down to the last one")
        void reservesTheLastCopies() {
            for (long member = 1; member <= 5; member++) {
                assertThat(reserve(1, member, 2)).hasStatus(HttpStatus.CREATED);
            }

            assertThat(stockOf(1)).isZero();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a reservation over the limit of 2 copies fails with 409 RESERVATION_LIMIT_EXCEEDED")
        void rejectsMoreThanTheLimit() {
            reserve(1, 1, 2);

            MvcTestResult result = reserve(1, 1, 1);

            assertThat(Responses.outcome(result)).isEqualTo("409 RESERVATION_LIMIT_EXCEEDED");
            assertThat(stockOf(1)).isEqualTo(8);
            assertThat(reservationsOf(1, 1)).isEqualTo(2);
        }

        @Test
        @DisplayName("asking for 3 copies at once is over the limit too, and changes nothing")
        void rejectsThreeAtOnce() {
            MvcTestResult result = reserve(1, 1, 3);

            assertThat(Responses.outcome(result)).isEqualTo("409 RESERVATION_LIMIT_EXCEEDED");
            assertThat(stockOf(1)).isEqualTo(10);
            assertThat(reservationsOf(1, 1)).isZero();
        }

        @Test
        @DisplayName("a reservation of a book that is out of stock fails with 409 OUT_OF_STOCK, and changes nothing")
        void rejectsABookThatIsOutOfStock() {
            MvcTestResult result = reserve(59, 1, 1);

            assertThat(Responses.outcome(result)).isEqualTo("409 OUT_OF_STOCK");
            assertThat(reservationsOf(59, 1)).isZero();
        }

        @Test
        @DisplayName("a reservation of a book that doesn't exist fails with 404 BOOK_NOT_FOUND")
        void unknownBook() {
            assertThat(Responses.outcome(reserve(9_999, 1, 1))).isEqualTo("404 BOOK_NOT_FOUND");
        }

        @Test
        @DisplayName("a reservation for an unknown member fails with 404 MEMBER_NOT_FOUND, and changes nothing")
        void unknownMember() {
            MvcTestResult result = reserve(1, 9_999, 1);

            assertThat(Responses.outcome(result)).isEqualTo("404 MEMBER_NOT_FOUND");
            assertThat(stockOf(1)).isEqualTo(10);
        }

        @Test
        @DisplayName("while another transaction holds the stock's lock, a reservation fails after the lock timeout "
                + "with 409 STOCK_BUSY, and changes nothing")
        void stockBusy() throws InterruptedException {
            MvcTestResult result;
            try (var lock = new StockLock(1)) {
                result = reserve(1, 1, 1); // waits for the lock until MySQL gives up after 2 seconds
            }

            assertThat(result)
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Another request is changing the stock of book 1. Try again in a moment.",
                              "instance": "/api/books/1/reservations",
                              "code": "STOCK_BUSY"
                            }""");
            assertThat(stockOf(1)).isEqualTo(10);
            assertThat(reservationsOf(1, 1)).isZero();
        }
    }

    @Nested
    @DisplayName("Concurrency")
    class Concurrency {

        @Test
        @DisplayName("20 members who reserve one copy each at the same moment get exactly the 10 copies in stock")
        void sellsExactlyTheStock() {
            Responses responses = Responses.sendAtOnce(20, i -> reserve(1, i + 1, 1));

            assertThat(responses.countByOutcome()).isEqualTo(Map.of("201", 10L, "409 OUT_OF_STOCK", 10L));
            assertThat(stockOf(1)).isZero();
        }

        @Test
        @DisplayName("a member who sends 10 reservations at the same moment gets exactly the 2 copies of the limit")
        void keepsTheLimitPerMember() {
            Responses responses = Responses.sendAtOnce(10, i -> reserve(1, 1, 1));

            assertThat(responses.countByOutcome())
                    .isEqualTo(Map.of("201", 2L, "409 RESERVATION_LIMIT_EXCEEDED", 8L));
            assertThat(reservationsOf(1, 1)).isEqualTo(2);
            assertThat(stockOf(1)).isEqualTo(8);
        }
    }

    /**
     * Holds the lock of a book's stock row in a transaction on another thread, the way a slow request would, until it
     * is closed. It lets go after 8 seconds at the latest, so a test can never leave the row locked.
     */
    private final class StockLock implements AutoCloseable {

        private final CountDownLatch release = new CountDownLatch(1);
        private final Thread thread;

        StockLock(long bookId) throws InterruptedException {
            var locked = new CountDownLatch(1);
            thread = Thread.ofPlatform().daemon().name("stock-lock").start(() ->
                    transactionTemplate.executeWithoutResult(status -> {
                        jdbc.sql("SELECT quantity FROM inventories WHERE book_id = ? FOR UPDATE").param(bookId)
                                .query(Integer.class).single();
                        locked.countDown();
                        try {
                            release.await(8, TimeUnit.SECONDS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }));
            if (!locked.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Could not lock the stock of book " + bookId);
            }
        }

        @Override
        public void close() throws InterruptedException {
            release.countDown();
            thread.join(Duration.ofSeconds(5));
        }
    }

    private MvcTestResult reserve(long bookId, long memberId, int quantity) {
        return mvc.post().uri("/api/books/{bookId}/reservations", bookId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "memberId": %d, "quantity": %d }""".formatted(memberId, quantity))
                .exchange();
    }

    private int stockOf(long bookId) {
        return jdbc.sql("SELECT quantity FROM inventories WHERE book_id = ?").param(bookId)
                .query(Integer.class).single();
    }

    private long reservationsOf(long bookId, long memberId) {
        return jdbc.sql("SELECT COALESCE(SUM(quantity), 0) FROM reservations WHERE book_id = ? AND member_id = ?")
                .param(bookId).param(memberId)
                .query(Long.class).single();
    }
}
