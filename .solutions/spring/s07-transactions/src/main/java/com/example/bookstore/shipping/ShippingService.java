package com.example.bookstore.shipping;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/** Ships paid orders. */
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final OrderRepository orderRepository;
    private final CourierClient courierClient;
    private final TransactionTemplate transactionTemplate;

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
    // No @Transactional here: a transaction holds a pooled connection from its first query until it ends. With the
    // courier call inside it, every shipment held a connection for seconds, and a few concurrent shipments used up
    // the whole pool; every other request then waited for a connection and timed out.
    public Shipment ship(long orderId) {
        // 1. A short transaction: check that the order can ship, and collect what the courier needs. The lazy
        //    member and lines must be read here, while the persistence context is open.
        PickupRequest pickup = transactionTemplate.execute(status -> {
            Order order = findOrder(orderId);
            if (!order.getStatus().canChangeTo(OrderStatus.SHIPPED)) {
                throw new BusinessException(ErrorCode.INVALID_ORDER_STATUS,
                        "Order %d is %s, so it can't be shipped.".formatted(orderId, order.getStatus()));
            }
            return PickupRequest.of(order);
        });

        // 2. The slow remote call, with no transaction and no connection held. If it fails, nothing has changed.
        String trackingNumber = courierClient.requestPickup(pickup);

        // 3. Another short transaction. ship() checks the status again: someone may have cancelled the order while
        //    we waited for the courier. Then this throws, and the booked pickup would need cancelling (a
        //    compensating action), which is the price of not holding locks during a remote call.
        transactionTemplate.executeWithoutResult(status -> findOrder(orderId).ship());
        return new Shipment(orderId, trackingNumber);
    }

    private Order findOrder(long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
    }
}
