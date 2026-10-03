package com.example.bookstore.order;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cancels orders that were never paid, so that the copies they hold go back on sale. {@link PendingOrderCleanupJob}
 * calls it on a schedule; tests call it directly.
 */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(PendingOrderCleanupProperties.class)
public class PendingOrderCleanupService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PendingOrderCleanupProperties properties;
    private final Clock clock;

    /**
     * Cancels every {@code PENDING} order that was ordered more than {@code maxAge} ago, and puts the copies of its
     * lines back into stock.
     *
     * <ul>
     *   <li>"Now" comes from the application's {@code Clock}. An order exactly {@code maxAge} old stays
     *       {@code PENDING}; one that is older, even by a microsecond, is cancelled.</li>
     *   <li>Orders in any other status are never touched.</li>
     *   <li>One transaction: all expired orders are cancelled, or none.</li>
     * </ul>
     *
     * @return how many orders were cancelled
     */
    @Transactional
    public int cancelExpiredOrders() {
        // The clock, never Instant.now(): a test can then put "now" anywhere, and the job behaves the same on every
        // run. findBy…Before is strictly before, so an order exactly maxAge old stays.
        Instant cutoff = clock.instant().minus(properties.maxAge());
        List<Order> expired = orderRepository.findByStatusAndOrderedAtBeforeOrderById(OrderStatus.PENDING, cutoff);
        for (Order order : expired) {
            order.cancel();
            // Order.cancel() leaves the stock to the caller, because it belongs to another aggregate.
            for (OrderLine line : order.getLines()) {
                Inventory inventory = inventoryRepository.findById(line.getBook().getId())
                        .orElseThrow(() -> new IllegalStateException("Book %d has no inventory"
                                .formatted(line.getBook().getId())));
                inventory.increase(line.getQuantity());
            }
        }
        return expired.size();
    }
}
