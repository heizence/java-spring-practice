package com.example.bookstore.notification;

import com.example.bookstore.payment.OrderPaid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** Emails a confirmation to the member when an order is paid. */
@Component
@RequiredArgsConstructor
public class OrderConfirmationListener {

    private final Mailer mailer;

    /**
     * Sends the confirmation of an {@link OrderPaid} event:
     * <ul>
     *   <li>to the event's email address,</li>
     *   <li>with the subject {@code Order <id> is paid}, such as {@code Order 201 is paid},</li>
     *   <li>and the body {@code We received your payment of <amount> won for order <id>. Thank you!}</li>
     * </ul>
     *
     * <p><b>Only after the payment is committed.</b> An email can't be taken back, so a payment that is rolled back
     * sends nothing, and nothing is sent while the payment's transaction is still running. A failure to send never
     * undoes the payment.
     */
    // A plain @EventListener would run inside pay(), at publishEvent(): the email would go out before the commit,
    // even for a payment that is rolled back later, and a mailer exception would roll the payment back.
    // @TransactionalEventListener holds the event until the transaction ends, and AFTER_COMMIT (the default phase)
    // calls this method only if it committed. Spring logs an exception thrown here instead of passing it on: the
    // payment is committed already. The email is best effort: if the application stops right after the commit, it
    // is never sent. S09-04's outbox closes that gap.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaid event) {
        mailer.send(new Email(event.email(),
                "Order %d is paid".formatted(event.orderId()),
                "We received your payment of %d won for order %d. Thank you!".formatted(event.amount(),
                        event.orderId())));
    }
}
