package com.example.bookstore.shipping;

import java.time.Clock;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.outbox.Message;
import com.example.bookstore.outbox.MessagePublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Mutant: a "dual write". It publishes to the broker inside the transaction instead of writing to the outbox. */
@Service
@RequiredArgsConstructor
public class ShippingService {

    private final OrderRepository orderRepository;
    private final MessagePublisher messagePublisher;
    private final JsonMapper jsonMapper;
    private final Clock clock;

    @Transactional
    public void ship(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
        order.ship();
        var event = new OrderShipped(orderId, order.getMember().getId(), clock.instant());
        messagePublisher.publish(new Message(0, "OrderShipped", orderId, jsonMapper.writeValueAsString(event)));
    }
}
