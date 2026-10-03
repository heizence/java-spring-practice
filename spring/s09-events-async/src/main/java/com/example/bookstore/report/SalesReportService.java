package com.example.bookstore.report;

import java.time.Clock;
import java.time.YearMonth;
import java.util.concurrent.CompletableFuture;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

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
    public CompletableFuture<SalesReport> generate(YearMonth month) {
        // TODO: S09-02
        throw new UnsupportedOperationException("TODO: S09-02");
    }
}
