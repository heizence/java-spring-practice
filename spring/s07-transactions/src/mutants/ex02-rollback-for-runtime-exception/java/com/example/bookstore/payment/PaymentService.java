package com.example.bookstore.payment;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.wallet.Wallet;
import com.example.bookstore.wallet.WalletRepository;
import com.example.bookstore.wallet.WalletTransactionRepository;
import com.example.bookstore.wallet.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Mutant: rollbackFor = RuntimeException.class, which is the default already and leaves out checked exceptions. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final PaymentAttemptLog paymentAttemptLog;

    /**
     * Pays a {@code PENDING} order from the member's wallet, in these steps:
     * <ol>
     *   <li>saves an {@code APPROVED} payment of the order's total. This comes first on purpose: the idempotency key
     *       is unique, so a retried request with the same key fails here, before anything else changes.</li>
     *   <li>marks the order {@code PAID};</li>
     *   <li>takes the total from the member's wallet and saves the {@code PAYMENT} wallet transaction.</li>
     * </ol>
     *
     * <p><b>All or nothing.</b> If any step fails, nothing changes: no payment, the order stays {@code PENDING}, the
     * wallet keeps its money. The only thing that remains of a declined payment is its entry in the
     * {@link PaymentAttemptLog}.
     *
     * @param idempotencyKey the client's key for this payment request; at most 64 characters
     * @return the approved payment
     * @throws PaymentDeclinedException if the wallet balance is too low to pay the order
     * @throws BusinessException        {@link ErrorCode#ORDER_NOT_FOUND} if there is no such order,
     *                                  {@link ErrorCode#INVALID_ORDER_STATUS} unless the order is {@code PENDING}
     */
    @Transactional(rollbackFor = RuntimeException.class)
    public PaymentResponse pay(long orderId, String idempotencyKey) throws PaymentDeclinedException {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
        long amount = order.getTotalAmount();
        long memberId = order.getMember().getId();

        Payment payment = paymentRepository.save(Payment.approved(order, idempotencyKey, amount));
        order.markPaid();

        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("Member %d has no wallet".formatted(memberId)));
        try {
            walletTransactionRepository.save(wallet.withdraw(amount, WalletTransactionType.PAYMENT));
        } catch (BusinessException insufficientBalance) {
            recordFailedAttempt(orderId, memberId, amount, insufficientBalance.getErrorCode());
            throw new PaymentDeclinedException(orderId, insufficientBalance.getMessage());
        }
        log.info("Order {} paid: {} won", orderId, amount);
        return PaymentResponse.from(payment);
    }

    /** The log is for compliance: failing to write it must never change the outcome of the payment. */
    private void recordFailedAttempt(long orderId, long memberId, long amount, ErrorCode reason) {
        try {
            paymentAttemptLog.recordFailure(orderId, memberId, amount, reason);
        } catch (RuntimeException e) {
            log.warn("Could not record the failed payment attempt for order {}", orderId, e);
        }
    }
}
