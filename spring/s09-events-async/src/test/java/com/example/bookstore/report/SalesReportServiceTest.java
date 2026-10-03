package com.example.bookstore.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.time.Instant;
import java.time.YearMonth;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * {@link SalesReportService#generate} runs on the report executor and reports through a future.
 *
 * <p>The tests wait for each future with a timeout, never with a sleep. They read the log output
 * ({@link OutputCaptureExtension}) to see which thread wrote the report's log line, and with which request id. The
 * report only reads committed seed data, so these tests can keep the usual rolled-back test transaction: the report's
 * own transaction runs on another thread.
 */
@IntegrationTest
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("S09-02 · Async methods and thread pools")
class SalesReportServiceTest {

    private static final YearMonth SEPTEMBER = YearMonth.of(2026, 9);
    private static final YearMonth OCTOBER = YearMonth.of(2026, 10); // the test clock's month

    @Autowired
    private SalesReportService salesReportService;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcClient jdbc;

    @AfterEach
    void clearMdc() {
        MDC.clear();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("reports the PAID and SHIPPED orders of a past month and their revenue")
        void reportsAPastMonth() throws Exception {
            var expected = jdbc.sql("""
                    SELECT COUNT(*) AS orders, COALESCE(SUM(total_amount), 0) AS revenue FROM orders
                    WHERE status IN ('PAID', 'SHIPPED')
                      AND ordered_at >= '2026-09-01 00:00:00' AND ordered_at < '2026-10-01 00:00:00'""")
                    .query((row, rowNumber) -> new SalesReport(SEPTEMBER, row.getLong("orders"),
                            row.getLong("revenue"), TestClock.DEFAULT_INSTANT))
                    .single();

            SalesReport report = startReport(SEPTEMBER).get(5, TimeUnit.SECONDS);

            assertThat(report).isEqualTo(expected);
            assertThat(report.paidOrders()).isPositive();
        }

        @Test
        @DisplayName("runs on a thread of the report executor (report-…), not on the caller's thread")
        void runsOnAReportThread(CapturedOutput output) throws Exception {
            startReport(SEPTEMBER).get(5, TimeUnit.SECONDS);

            assertThat(logLineOfTheReport(output, SEPTEMBER)).contains("report-");
        }

        @Test
        @DisplayName("the report's log line carries the caller's request id")
        void logLineCarriesTheRequestId(CapturedOutput output) throws Exception {
            MDC.put("requestId", "req-42");

            startReport(SEPTEMBER).get(5, TimeUnit.SECONDS);

            assertThat(logLineOfTheReport(output, SEPTEMBER)).contains("[req-42]");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a month without orders reports zeros")
        void monthWithoutOrders() throws Exception {
            SalesReport report = startReport(YearMonth.of(2026, 1)).get(5, TimeUnit.SECONDS);

            assertThat(report.paidOrders()).isZero();
            assertThat(report.revenue()).isZero();
        }

        @Test
        @DisplayName("a month can be reported from the first moment of the next month")
        void monthJustOver() throws Exception {
            clock.setInstant(Instant.parse("2026-10-01T00:00:00Z"));

            SalesReport report = startReport(SEPTEMBER).get(5, TimeUnit.SECONDS);

            assertThat(report.month()).isEqualTo(SEPTEMBER);
            assertThat(report.generatedAt()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z"));
        }

        @Test
        @DisplayName("each task on the report executor gets the caller's MDC, and none is left behind for the next")
        void executorPassesAndClearsTheMdc() throws Exception {
            ThreadPoolTaskExecutor executor = reportExecutor();
            MDC.put("requestId", "req-1");
            var bothThreadsBusy = new CountDownLatch(2);
            Future<String> first = executor.submit(() -> mdcWhenBothThreadsAreBusy(bothThreadsBusy));
            Future<String> second = executor.submit(() -> mdcWhenBothThreadsAreBusy(bothThreadsBusy));
            assertThat(first.get(5, TimeUnit.SECONDS)).isEqualTo("req-1");
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo("req-1");
            MDC.clear();

            // Both threads have run a task with req-1. Whichever one runs this task must not see it any more.
            String seenByTheNextTask = executor.submit(() -> MDC.get("requestId")).get(5, TimeUnit.SECONDS);

            assertThat(seenByTheNextTask).isNull();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a report of the current month fails through the future with REPORT_NOT_READY")
        void currentMonthIsNotReady() {
            CompletableFuture<SalesReport> future = startReport(OCTOBER);

            Throwable failure = catchThrowable(() -> future.get(5, TimeUnit.SECONDS));

            assertThat(failure).isInstanceOf(ExecutionException.class);
            assertThat(failure.getCause()).isInstanceOfSatisfying(BusinessException.class,
                    cause -> assertThat(cause.getErrorCode()).isEqualTo(ErrorCode.REPORT_NOT_READY));
        }

        @Test
        @DisplayName("a null month fails through the future with IllegalArgumentException, not on the caller's thread")
        void nullMonth() {
            CompletableFuture<SalesReport> future = startReport(null);

            Throwable failure = catchThrowable(() -> future.get(5, TimeUnit.SECONDS));

            assertThat(failure).isInstanceOf(ExecutionException.class)
                    .cause().isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("Thread pool")
    class ThreadPool {

        @Test
        @DisplayName("reportExecutor has exactly 2 threads named report-…, and a queue for 10 waiting reports")
        void reportExecutorSettings() {
            ThreadPoolTaskExecutor executor = reportExecutor();

            assertThat(executor.getCorePoolSize()).isEqualTo(2);
            assertThat(executor.getMaxPoolSize()).isEqualTo(2);
            assertThat(executor.getQueueCapacity()).isEqualTo(10);
            assertThat(executor.getThreadNamePrefix()).isEqualTo("report-");
        }
    }

    /** Calls {@code generate}, which must return a future at once, even when the report is going to fail. */
    private CompletableFuture<SalesReport> startReport(YearMonth month) {
        try {
            return salesReportService.generate(month);
        } catch (RuntimeException e) {
            throw new AssertionError("generate(%s) threw %s on the caller's thread; it must return a future instead"
                    .formatted(month, e), e);
        }
    }

    private ThreadPoolTaskExecutor reportExecutor() {
        return context.getBean("reportExecutor", ThreadPoolTaskExecutor.class);
    }

    /** Waits until both pool threads run such a task, so that each thread has seen the caller's MDC once. */
    private static String mdcWhenBothThreadsAreBusy(CountDownLatch bothThreadsBusy) throws InterruptedException {
        bothThreadsBusy.countDown();
        if (!bothThreadsBusy.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("The two tasks didn't run at the same time");
        }
        return MDC.get("requestId");
    }

    private static String logLineOfTheReport(CapturedOutput output, YearMonth month) {
        String text = "Sales report for " + month;
        return output.getOut().lines()
                .filter(line -> line.contains(text))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No log line contains '%s'".formatted(text)));
    }
}
