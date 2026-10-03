package com.example.bookstore.shipping;

import java.time.Clock;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.outbox.OutboxEvent;
import com.example.bookstore.outbox.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Ships paid orders, and tells the delivery-tracking service about it through the transactional outbox. */
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    /**
     * Marks a {@code PAID} order {@code SHIPPED} and records an {@link OrderShipped} event in the outbox.
     *
     * <ul>
     *   <li>The outbox event: aggregate type {@code Order}, aggregate id the order's id, event type
     *       {@code OrderShipped}, and as payload the {@link OrderShipped} record as JSON (the order's id, the
     *       member's id, and "now" by the {@code Clock}). Write it with {@code jsonMapper.writeValueAsString}.</li>
     *   <li><b>One transaction</b> for the order and its event: both are committed, or neither is.</li>
     *   <li>This method never calls the message broker. {@code OutboxRelay} publishes the event after the
     *       commit.</li>
     * </ul>
     *
     * @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if there is no such order,
     *                           {@link ErrorCode#INVALID_ORDER_STATUS} unless the order is {@code PAID}; nothing
     *                           changes then
     */
    // Publishing to the broker right here would be a "dual write": the database and the broker are two systems, and
    // no transaction covers both. The message could go out for a shipment that is then rolled back, or the commit
    // could succeed while the broker is down, and the message would be lost. A row in the same transaction can't
    // disagree with the order.
    @Transactional
    public void ship(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
        order.ship();
        var event = new OrderShipped(orderId, order.getMember().getId(), clock.instant());
        outboxEventRepository.save(OutboxEvent.create("Order", orderId, "OrderShipped",
                jsonMapper.writeValueAsString(event)));
    }
}
