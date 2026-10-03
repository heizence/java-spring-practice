package com.example.bookstore.notification;

import com.example.bookstore.payment.OrderPaid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Mutant: AFTER_COMPLETION runs after a commit and after a rollback alike. */
@Component
@RequiredArgsConstructor
public class OrderConfirmationListener {

    private final Mailer mailer;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void onOrderPaid(OrderPaid event) {
        mailer.send(new Email(event.email(),
                "Order %d is paid".formatted(event.orderId()),
                "We received your payment of %d won for order %d. Thank you!".formatted(event.amount(),
                        event.orderId())));
    }
}
