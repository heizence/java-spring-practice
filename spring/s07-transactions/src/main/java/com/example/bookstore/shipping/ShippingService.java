package com.example.bookstore.shipping;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Ships paid orders. */
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final OrderRepository orderRepository;
    private final CourierClient courierClient;

    /**
     * Ships a paid order: books a pickup with the courier, then marks the order {@code SHIPPED}.
     *
     * <ul>
     *   <li>Only a {@code PAID} order can ship. Any other order is rejected before the courier is called.</li>
     *   <li>If the courier fails, its exception reaches the caller, and the order stays {@code PAID}.</li>
     *   <li>The courier's API is a remote call that can take seconds. It runs outside any database transaction:
     *       this method holds a pooled database connection only for the short database work before and after
     *       the call.</li>
     * </ul>
     *
     * @return the order's id and the courier's tracking number
     * @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if there is no such order,
     *                           {@link ErrorCode#INVALID_ORDER_STATUS} unless the order is {@code PAID}
     */
    @Transactional
    public Shipment ship(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
        order.ship(); // PAID → SHIPPED; any other status throws INVALID_ORDER_STATUS
        String trackingNumber = courierClient.requestPickup(PickupRequest.of(order));
        return new Shipment(orderId, trackingNumber);
    }
}
