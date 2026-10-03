package com.example.bookstore.common.timing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.time.Duration;

import com.example.bookstore.book.BookService;
import com.example.bookstore.support.FakeTicker;
import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@IntegrationTest
@Import(TimingAspectTest.SalesReportsConfiguration.class)
@DisplayName("S03-01 · Your first aspect")
class TimingAspectTest {

    @Autowired
    private SalesReports reports;

    @Autowired
    private MethodTimings timings;

    @Autowired
    private BookService bookService;

    @Autowired
    private FakeTicker ticker;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("records the name and the duration of a call to a @Timed method")
        void recordsNameAndDuration() {
            reports.monthlySales(9);

            assertThat(timings.all())
                    .containsExactly(new MethodTiming("SalesReports.monthlySales", Duration.ofMillis(250), false));
        }

        @Test
        @DisplayName("returns the method's result unchanged")
        void returnsTheResult() {
            long sales = reports.monthlySales(9);

            assertThat(sales).isEqualTo(9_000);
        }

        @Test
        @DisplayName("records the timing under the name given in @Timed(\"…\")")
        void usesTheGivenName() {
            reports.yearlySales();

            assertThat(timings.all())
                    .containsExactly(new MethodTiming("reports.yearly", Duration.ofSeconds(2), false));
        }

        @Test
        @DisplayName("times the catalog search as book.search")
        void timesTheCatalogSearch() {
            bookService.search("java");

            assertThat(timings.all()).extracting(MethodTiming::name).containsExactly("book.search");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("doesn't time a method without @Timed")
        void ignoresMethodsWithoutTheAnnotation() {
            reports.dailySales();

            assertThat(timings.all()).isEmpty();
        }

        @Test
        @DisplayName("records every call separately, in the order the calls ended")
        void recordsEveryCall() {
            reports.monthlySales(1);
            reports.yearlySales();
            reports.monthlySales(2);

            assertThat(timings.all()).containsExactly(
                    new MethodTiming("SalesReports.monthlySales", Duration.ofMillis(250), false),
                    new MethodTiming("reports.yearly", Duration.ofSeconds(2), false),
                    new MethodTiming("SalesReports.monthlySales", Duration.ofMillis(250), false));
        }

        @Test
        @DisplayName("times a method that returns nothing (void)")
        void timesVoidMethods() {
            reports.refresh();

            assertThat(timings.all())
                    .containsExactly(new MethodTiming("SalesReports.refresh", Duration.ofMillis(40), false));
        }

        @Test
        @DisplayName("measures each call on its own, however much time passed before it")
        void measuresFromTheStartOfTheCall() {
            ticker.advance(Duration.ofMinutes(5)); // time passes between requests

            reports.monthlySales(1);

            assertThat(timings.all()).extracting(MethodTiming::elapsed).containsExactly(Duration.ofMillis(250));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("lets an exception through unchanged: the caller gets the very same object")
        void rethrowsTheSameException() {
            Throwable thrown = catchThrowable(reports::exportCsv);

            assertThat(thrown).isSameAs(SalesReports.DISK_FULL);
        }

        @Test
        @DisplayName("lets a checked exception through as itself, not wrapped in another exception")
        void rethrowsCheckedExceptions() {
            assertThatThrownBy(reports::archive)
                    .isExactlyInstanceOf(IOException.class)
                    .hasMessage("archive server down");
        }

        @Test
        @DisplayName("records a call that throws as failed, with the time it took until it threw")
        void recordsFailedCalls() {
            catchThrowable(reports::exportCsv);

            assertThat(timings.all())
                    .containsExactly(new MethodTiming("SalesReports.exportCsv", Duration.ofMillis(300), true));
        }

        @Test
        @DisplayName("records a call that throws a checked exception as failed too")
        void recordsCallsThatThrowCheckedExceptions() {
            catchThrowable(reports::archive);

            assertThat(timings.all())
                    .containsExactly(new MethodTiming("SalesReports.archive", Duration.ofMillis(100), true));
        }
    }

    /**
     * A slow reporting service for these tests. Each method moves the fake ticker forward, to play the time its work
     * takes, so every duration the aspect measures is exact.
     */
    static class SalesReports {

        /** Thrown by {@link #exportCsv()}, so that a test can check that it's the very same object. */
        static final IllegalStateException DISK_FULL = new IllegalStateException("disk full");

        private final FakeTicker ticker;

        SalesReports(FakeTicker ticker) {
            this.ticker = ticker;
        }

        @Timed
        public long monthlySales(int month) {
            ticker.advance(Duration.ofMillis(250));
            return month * 1_000L;
        }

        @Timed("reports.yearly")
        public long yearlySales() {
            ticker.advance(Duration.ofSeconds(2));
            return 12_000;
        }

        @Timed
        public void refresh() {
            ticker.advance(Duration.ofMillis(40));
        }

        public long dailySales() {
            ticker.advance(Duration.ofMillis(10));
            return 40;
        }

        @Timed
        public void exportCsv() {
            ticker.advance(Duration.ofMillis(300));
            throw DISK_FULL;
        }

        @Timed
        public void archive() throws IOException {
            ticker.advance(Duration.ofMillis(100));
            throw new IOException("archive server down");
        }
    }

    /** Adds {@link SalesReports} to the application context of this test class (see {@code @Import} above). */
    @TestConfiguration(proxyBeanMethods = false)
    static class SalesReportsConfiguration {

        @Bean
        SalesReports salesReports(FakeTicker ticker) {
            return new SalesReports(ticker);
        }
    }
}
