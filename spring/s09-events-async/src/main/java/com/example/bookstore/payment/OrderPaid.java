package com.example.bookstore.payment;

import java.time.Instant;

/**
 * An application event: an order was paid. {@link PaymentService#pay} publishes it.
 *
 * <p>Events carry plain values, never entities. A listener may run after the transaction has ended (S09-01), when
 * the entities of that transaction are detached and their lazy associations can't load any more. So the event
 * carries everything its listeners need, including the email address for the confirmation.
 *
 * @param email  the member's email address
 * @param amount the amount paid, in won
 * @param paidAt when the payment was made
 */
public record OrderPaid(long orderId, long memberId, String email, long amount, Instant paidAt) {
}
