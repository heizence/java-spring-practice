package com.example.bookstore.payment;

import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The audit log of failed payment attempts, which compliance requires: which order, which member, how much, and why
 * it failed. {@link PaymentService} writes to it when it declines a payment.
 */
@Service
@RequiredArgsConstructor
public class PaymentAttemptLog {

    private final PaymentAttemptRepository paymentAttemptRepository;

    /**
     * Saves a {@link PaymentAttempt} for a payment that failed.
     *
     * <ul>
     *   <li><b>It survives the caller's rollback.</b> The payment that failed is rolled back, but its attempt stays
     *       in the log.</li>
     *   <li><b>It is committed before this method returns,</b> so other database sessions (support staff, a fraud
     *       check) see it at once, even while the caller's transaction is still running.</li>
     *   <li><b>A failure here never dooms the caller's transaction.</b> If saving the attempt fails, the exception
     *       reaches the caller, and the caller can catch it and still commit its own work.</li>
     * </ul>
     *
     * @param orderId   the order that couldn't be paid
     * @param memberId  the member who tried to pay
     * @param amount    the amount in won; must be positive
     * @param errorCode why the payment failed
     * @throws IllegalArgumentException if {@code amount} isn't positive or {@code errorCode} is {@code null}
     */
    public void recordFailure(long orderId, long memberId, long amount, ErrorCode errorCode) {
        // TODO: S07-03
        throw new UnsupportedOperationException("TODO: S07-03");
    }
}
