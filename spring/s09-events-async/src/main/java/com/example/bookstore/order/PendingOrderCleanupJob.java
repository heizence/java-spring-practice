package com.example.bookstore.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Runs the pending-order cleanup on a schedule.
 *
 * <p>The job only decides <em>when</em>; {@link PendingOrderCleanupService} decides <em>what</em>. So tests check the
 * cleanup by calling the service with a test clock, and check the schedule separately.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderCleanupJob {

    private final PendingOrderCleanupService cleanupService;

    /**
     * Calls {@link PendingOrderCleanupService#cancelExpiredOrders()} with a fixed delay of
     * {@code bookstore.orders.pending-cleanup.interval} between the end of one run and the start of the next. The
     * first run starts one interval after the application has started. Logs how many orders it cancelled, if any.
     */
    public void run() {
        // TODO: S09-03
        throw new UnsupportedOperationException("TODO: S09-03");
    }
}
