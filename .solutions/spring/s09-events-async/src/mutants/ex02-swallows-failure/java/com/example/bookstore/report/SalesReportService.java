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

/** Mutant: catches every failure in the background, logs it, and completes the future with an empty report. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SalesReportService {

    private final OrderRepository orderRepository;
    private final Clock clock;

    @Async("reportExecutor")
    @Transactional(readOnly = true)
    public CompletableFuture<SalesReport> generate(YearMonth month) {
        try {
            Assert.notNull(month, "month must not be null");
            if (!month.isBefore(YearMonth.now(clock))) {
                throw new BusinessException(ErrorCode.REPORT_NOT_READY,
                        "The sales report for %s is available from %s.".formatted(month,
                                month.plusMonths(1).atDay(1)));
            }
            Instant from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            Instant to = month.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
            OrderTotals totals = orderRepository.sumOrders(EnumSet.of(OrderStatus.PAID, OrderStatus.SHIPPED), from,
                    to);

            log.info("Sales report for {}: {} paid orders, {} won", month, totals.orders(), totals.amount());
            return CompletableFuture.completedFuture(
                    new SalesReport(month, totals.orders(), totals.amount(), clock.instant()));
        } catch (RuntimeException e) {
            log.error("Could not generate the sales report for {}", month, e);
            return CompletableFuture.completedFuture(new SalesReport(month, 0, 0, clock.instant()));
        }
    }
}
