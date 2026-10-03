package com.example.bookstore.order;

import java.time.Clock;
import java.time.Instant;
import java.util.List;

import com.example.bookstore.inventory.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mutant: cancels the orders, but forgets to put their copies back into stock. */
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
        Instant cutoff = clock.instant().minus(properties.maxAge());
        List<Order> expired = orderRepository.findByStatusAndOrderedAtBeforeOrderById(OrderStatus.PENDING, cutoff);
        expired.forEach(Order::cancel);
        return expired.size();
    }
}
