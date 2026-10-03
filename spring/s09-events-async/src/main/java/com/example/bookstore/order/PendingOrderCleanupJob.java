package com.example.bookstore.order;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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
    // fixedDelay, not fixedRate: the next run is planned from the end of the previous one, so two runs never overlap
    // on this instance. The placeholders read the interval from the configuration ("10m"). An exception thrown here
    // is logged by the scheduler, and the next run happens as planned.
    @Scheduled(fixedDelayString = "${bookstore.orders.pending-cleanup.interval}",
            initialDelayString = "${bookstore.orders.pending-cleanup.interval}")
    public void run() {
        int cancelled = cleanupService.cancelExpiredOrders();
        if (cancelled > 0) {
            log.info("Cancelled {} unpaid orders", cancelled);
        }
    }
}
