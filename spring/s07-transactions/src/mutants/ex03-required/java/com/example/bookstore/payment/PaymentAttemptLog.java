package com.example.bookstore.payment;

import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mutant: the default propagation, REQUIRED, so the entry joins the caller's transaction. */
@Service
@RequiredArgsConstructor
public class PaymentAttemptLog {

    private final PaymentAttemptRepository paymentAttemptRepository;

    @Transactional
    public void recordFailure(long orderId, long memberId, long amount, ErrorCode errorCode) {
        paymentAttemptRepository.save(PaymentAttempt.failed(orderId, memberId, amount, errorCode));
    }
}
