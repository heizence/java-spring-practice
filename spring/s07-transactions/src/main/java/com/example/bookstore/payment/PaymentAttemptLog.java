package com.example.bookstore.payment;

import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    // REQUIRES_NEW suspends the caller's transaction and runs this method in a new one, on a second connection. All
    // three rules above follow from that:
    // - the new transaction commits when this method returns, so other sessions see the entry at once;
    // - the caller's later rollback can't undo a transaction that has already committed;
    // - an exception thrown here rolls back only the new transaction. With the default, REQUIRED, this method would
    //   join the caller's transaction, and the same exception would mark it rollback-only: the caller's commit would
    //   then fail with UnexpectedRollbackException, even if it caught the exception.
    // The parameters are ids, not entities: the new transaction has its own persistence context, in which the
    // caller's entities are unknown.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(long orderId, long memberId, long amount, ErrorCode errorCode) {
        paymentAttemptRepository.save(PaymentAttempt.failed(orderId, memberId, amount, errorCode));
    }
}
