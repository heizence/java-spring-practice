package com.example.bookstore.shipping;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mutant: the courier call runs on another thread, so it is "outside the transaction", but the transaction waits
 * for it and holds its connection all the while.
 */
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final OrderRepository orderRepository;
    private final CourierClient courierClient;

    @Transactional
    public Shipment ship(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
        order.ship();
        PickupRequest pickup = PickupRequest.of(order);
        try {
            String trackingNumber = CompletableFuture.supplyAsync(() -> courierClient.requestPickup(pickup)).join();
            return new Shipment(orderId, trackingNumber);
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }
}
