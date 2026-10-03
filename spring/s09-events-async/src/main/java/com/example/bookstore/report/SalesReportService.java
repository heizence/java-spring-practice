package com.example.bookstore.report;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.concurrent.CompletableFuture;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.order.OrderStatus;
import com.example.bookstore.order.OrderTotals;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/** Monthly sales reports for the finance team. A report scans a month of orders, so it runs in the background. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesReportService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    /**
     * Generates the sales report of a month on the report executor's threads ({@code report-1}, {@code report-2}),
     * and returns at once with a future of the report.
     *
     * <ul>
     *   <li>The report counts the orders that are {@code PAID} or {@code SHIPPED} and were ordered in that month
     *       (UTC), and adds up their totals ({@code OrderRepository.sumOrders}).</li>
     *   <li>It logs one line at INFO level, such as {@code Sales report for 2026-09: 41 paid orders, 1234000 won}.
     *       Like every log line of the report thread, it carries the caller's request id ({@link
     *       com.example.bookstore.common.MdcTaskDecorator}).</li>
     *   <li>Only a month that is over can be reported. The current month (by the {@code Clock}) and later ones fail
     *       with {@link ErrorCode#REPORT_NOT_READY}.</li>
     *   <li><b>Every failure reaches the caller through the future,</b> never as an exception thrown by this method:
     *       {@link BusinessException} for a month that isn't over, {@link IllegalArgumentException} for a
     *       {@code null} month.</li>
     * </ul>
     *
     * @param month the month to report
     * @return a future that completes with the report, or completes exceptionally with the reason it failed
     */
    // The proxy hands the whole call to the "reportExecutor" bean and returns a future at once; the method body runs
    // on a report- thread. Without the name, @Async would use Spring Boot's applicationTaskExecutor. The
    // transaction starts on the report thread too: @Async's interceptor comes first, @Transactional's second.
    // Anything thrown in here, including the argument check, completes the caller's future exceptionally.
    @Async("reportExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<SalesReport> generate(YearMonth month) {
        Assert.notNull(month, "month must not be null");
        if (!month.isBefore(YearMonth.now(clock))) {
            throw new BusinessException(ErrorCode.REPORT_NOT_READY,
                    "The sales report for %s is available from %s.".formatted(month, month.plusMonths(1).atDay(1)));
        }
        Instant from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        OrderTotals totals = orderRepository.sumOrders(EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED), from, to);

        log.info("Sales report for {}: {} paid orders, {} won", month, totals.orders(), totals.amount());
        // The proxy's future completes with this future's value. completedFuture only wraps the finished result.
        return CompletableFuture.completedFuture(
                new SalesReport(month, totals.orders(), totals.amount(), clock.instant()));
    }
}
