package com.example.bookstore.payment;

import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mutant: REQUIRES_NEW sits on a helper that recordFailure() calls on {@code this}. The call bypasses the proxy, so
 * the save joins the caller's transaction.
 */
@Service
@RequiredArgsConstructor
public class PaymentAttemptLog {

    private final PaymentAttemptRepository paymentAttemptRepository;

    public void recordFailure(long orderId, long memberId, long amount, ErrorCode errorCode) {
        save(PaymentAttempt.failed(orderId, memberId, amount, errorCode));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(PaymentAttempt attempt) {
        paymentAttemptRepository.save(attempt);
    }
}
