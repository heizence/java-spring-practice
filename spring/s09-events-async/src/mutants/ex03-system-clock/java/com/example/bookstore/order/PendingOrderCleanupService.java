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

/** Mutant: reads "now" from the system clock (Instant.now()) instead of the application's Clock. */
@Service
@RequiredArgsConstructor
@EnableConfigurationProperties(PendingOrderCleanupProperties.class)
public class PendingOrderCleanupService {

    private final OrderRepository orderRepository;
    private final InventoryRepository inventoryRepository;
    private final PendingOrderCleanupProperties properties;
    private final Clock clock;

    @Transactional
    public int cancelExpiredOrders() {
        Instant cutoff = Instant.now().minus(properties.maxAge());
        List<Order> expired = orderRepository.findByStatusAndOrderedAtBeforeOrderById(OrderStatus.PENDING, cutoff);
        for (Order order : expired) {
            order.cancel();
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
