package com.example.bookstore.shipping;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Mutant: the courier call is outside the transaction, but the order is marked SHIPPED in the first transaction,
 * before the courier has booked the pickup.
 */
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final OrderRepository orderRepository;
    private final CourierClient courierClient;
    private final TransactionTemplate transactionTemplate;

    public Shipment ship(long orderId) {
        PickupRequest pickup = transactionTemplate.execute(status -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                            "Order %d was not found.".formatted(orderId)));
            order.ship();
            return PickupRequest.of(order);
        });
        String trackingNumber = courierClient.requestPickup(pickup);
        return new Shipment(orderId, trackingNumber);
    }
}
