package com.example.bookstore.payment;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for payments. */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;

    /**
     * A payment with the status of its order.
     *
     * @throws BusinessException {@link ErrorCode#PAYMENT_NOT_FOUND} if no payment has this id
     */
    // Without a transaction here, findById() would run in a transaction of its own and return a detached payment:
    // reading its lazy order in PaymentResponse.from would then throw LazyInitializationException.
    @Transactional(readOnly = true)
    public PaymentResponse findPayment(long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND,
                        "Payment %d was not found.".formatted(paymentId)));
        return PaymentResponse.from(payment);
    }
}
