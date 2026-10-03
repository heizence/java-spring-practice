package com.example.bookstore.notification;

import com.example.bookstore.payment.OrderPaid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Mutant: a plain @EventListener runs at publishEvent(), inside the payment's transaction. */
@Component
@RequiredArgsConstructor
public class OrderConfirmationListener {

    private final Mailer mailer;

    @EventListener
    public void onOrderPaid(OrderPaid event) {
        mailer.send(new Email(event.email(),
                "Order %d is paid".formatted(event.orderId()),
                "We received your payment of %d won for order %d. Thank you!".formatted(event.amount(),
                        event.orderId())));
    }
}
