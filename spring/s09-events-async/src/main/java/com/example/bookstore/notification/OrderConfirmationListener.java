package com.example.bookstore.notification;

import com.example.bookstore.payment.OrderPaid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

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
    public void onOrderPaid(OrderPaid event) {
        // TODO: S09-01
        throw new UnsupportedOperationException("TODO: S09-01");
    }
}
