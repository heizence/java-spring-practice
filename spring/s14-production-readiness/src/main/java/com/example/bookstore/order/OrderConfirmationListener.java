package com.example.bookstore.order;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Confirms each placed order once its transaction has committed. */
@Component
@RequiredArgsConstructor
class OrderConfirmationListener {

    private final OrderConfirmationSender confirmationSender;

    /**
     * Runs after the commit: a confirmation for an order that was rolled back would announce an order that doesn't
     * exist. The sender runs on another thread, so the request doesn't wait for it.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void confirm(OrderPlaced event) {
        confirmationSender.send(event.orderId());
    }
}
