package com.example.bookstore.inventory;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.AbstractHealthIndicator;
import org.springframework.boot.health.contributor.Health;
import org.springframework.stereotype.Component;

/**
 * Whether this instance can read the stock, which every order needs. Actuator lists it as {@code inventory}: the
 * bean's name without "HealthIndicator".
 *
 * <ul>
 *   <li>{@code UP}, with the details {@code booksInStock} (books with at least one copy) and {@code booksOutOfStock}
 *       (books with none), from {@link InventoryRepository#countByQuantityGreaterThan} and
 *       {@link InventoryRepository#countByQuantity}.</li>
 *   <li>{@code DOWN} if reading the stock fails, with the exception in the detail {@code error}.</li>
 * </ul>
 *
 * <p>It belongs to the readiness group: an instance that can't read the stock shouldn't get customers. Sold-out books
 * don't make it {@code DOWN}: the shop still works, and an instance that refused traffic because of them would only
 * make things worse.
 */
@Component
@RequiredArgsConstructor
public class InventoryHealthIndicator extends AbstractHealthIndicator {

    private final InventoryRepository inventoryRepository;

    /**
     * {@link AbstractHealthIndicator} turns an exception thrown here into {@code DOWN}, with an {@code error}
     * detail.
     */
    @Override
    protected void doHealthCheck(Health.Builder builder) {
        builder.up()
                .withDetail("booksInStock", inventoryRepository.countByQuantityGreaterThan(0))
                .withDetail("booksOutOfStock", inventoryRepository.countByQuantity(0));
    }
}
