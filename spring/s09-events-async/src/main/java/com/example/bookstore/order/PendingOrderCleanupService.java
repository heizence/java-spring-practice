package com.example.bookstore.order;

import java.time.Clock;

import com.example.bookstore.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

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
    public int cancelExpiredOrders() {
        // TODO: S09-03
        throw new UnsupportedOperationException("TODO: S09-03");
    }
}
