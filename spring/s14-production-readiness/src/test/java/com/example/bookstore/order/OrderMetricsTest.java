package com.example.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.TimeUnit;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.support.IntegrationTest;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * The order metrics, read from the application's {@link MeterRegistry} after placing orders through the API. All
 * tests share one registry, so they compare counts before and after. Book 59 is out of stock.
 */
@IntegrationTest
@DisplayName("S14-02 · Health checks and metrics")
class OrderMetricsTest {

    private static final String PLACED = "bookstore.orders.placed";
    private static final String PLACEMENT = "bookstore.orders.placement";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private MeterRegistry meterRegistry;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("counts each placed order: bookstore.orders.placed")
        void countsPlacedOrders() {
            double before = placedCount();

            placeOrder(1, 1);
            placeOrder(2, 1);

            assertThat(placedCount()).isEqualTo(before + 2);
        }

        @Test
        @DisplayName("times each placement with outcome=placed: bookstore.orders.placement")
        void timesPlacements() {
            long before = placementCount("placed");

            placeOrder(1, 1);

            Timer timer = meterRegistry.get(PLACEMENT).tag("outcome", "placed").timer();
            assertThat(timer.count()).isEqualTo(before + 1);
            assertThat(timer.totalTime(TimeUnit.NANOSECONDS)).isPositive();
        }

        @Test
        @DisplayName("GET /actuator/metrics/bookstore.orders.placed shows the counter")
        void counterIsExposed() {
            placeOrder(1, 1);

            MvcTestResult result = mvc.get().uri("/actuator/metrics/" + PLACED).exchange();

            assertThat(result).hasStatusOk()
                    .bodyJson().extractingPath("$.measurements[0].statistic").isEqualTo("COUNT");
            assertThat(result).bodyJson().extractingPath("$.measurements[0].value").asNumber()
                    .satisfies(count -> assertThat(count.doubleValue()).isPositive());
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("keeps one time series for the counter, whoever orders: no ids as tags")
        void oneTimeSeries() {
            placeOrder(1, 1);
            placeOrder(2, 1);
            placeOrder(3, 1);

            assertThat(meterRegistry.find(PLACED).counters()).hasSize(1);
            assertThat(meterRegistry.find(PLACED).counter().getId().getTags()).isEmpty();
        }

        @Test
        @DisplayName("the counter exists, with 0, before the first order")
        void counterStartsAtZero() {
            var registry = new SimpleMeterRegistry();

            new OrderMetrics(registry);

            assertThat(registry.get(PLACED).counter().count()).isZero();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a rejected order isn't counted, and is timed with outcome=rejected")
        void rejectedOrder() {
            double placedBefore = placedCount();
            long rejectedBefore = placementCount("rejected");

            MvcTestResult result = mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                    .content("""
                            { "memberId": 1, "lines": [ { "bookId": 59, "quantity": 1 } ] }""")
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(placedCount()).isEqualTo(placedBefore);
            assertThat(placementCount("rejected")).isEqualTo(rejectedBefore + 1);
        }

        @Test
        @DisplayName("the placement's exception reaches the caller unchanged")
        void rethrowsTheException() {
            var metrics = new OrderMetrics(new SimpleMeterRegistry());
            var outOfStock = new BusinessException(ErrorCode.OUT_OF_STOCK);

            assertThatThrownBy(() -> metrics.recordPlacement(() -> {
                throw outOfStock;
            })).isSameAs(outOfStock);
        }
    }

    private void placeOrder(long memberId, long bookId) {
        assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "memberId": %d, "lines": [ { "bookId": %d, "quantity": 1 } ] }"""
                        .formatted(memberId, bookId)))
                .hasStatus(HttpStatus.CREATED);
    }

    /** 0 if the counter doesn't exist yet. */
    private double placedCount() {
        var counter = meterRegistry.find(PLACED).counter();
        return counter == null ? 0 : counter.count();
    }

    /** 0 if the timer doesn't exist yet. */
    private long placementCount(String outcome) {
        var timer = meterRegistry.find(PLACEMENT).tag("outcome", outcome).timer();
        return timer == null ? 0 : timer.count();
    }
}
