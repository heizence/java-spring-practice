package com.example.bookstore.payment;

import java.time.Clock;

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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Pays for orders from the members' wallets. */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final Clock clock;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Pays a {@code PENDING} order from the member's wallet: saves an {@code APPROVED} payment, marks the order
     * {@code PAID} and takes the total from the wallet. All or nothing: if any step fails, nothing changes.
     *
     * <p>When the order is paid, publishes an {@link OrderPaid} event with the order's id, the member's id and email
     * address, the amount and the current time. Listeners decide what happens next, such as a confirmation email.
     *
     * @param idempotencyKey the client's key for this payment request; at most 64 characters
     * @return the approved payment
     * @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if there is no such order,
     *                           {@link ErrorCode#INVALID_ORDER_STATUS} unless the order is {@code PENDING},
     *                           {@link ErrorCode#INSUFFICIENT_BALANCE} if the wallet can't pay the total
     */
    @Transactional
    public PaymentResponse pay(long orderId, String idempotencyKey) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
        long amount = order.getTotalAmount();
        long memberId = order.getMember().getId();

        Payment payment = paymentRepository.save(Payment.approved(order, idempotencyKey, amount));
        order.markPaid();
        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("Member %d has no wallet".formatted(memberId)));
        walletTransactionRepository.save(wallet.withdraw(amount, WalletTransactionType.PAYMENT));

        // The service only announces what happened; it doesn't know who listens. The email address is read here,
        // inside the transaction, because the listener may run after it, when lazy loading no longer works.
        eventPublisher.publishEvent(new OrderPaid(orderId, memberId, order.getMember().getEmail(), amount,
                clock.instant()));
        log.info("Order {} paid: {} won", orderId, amount);
        return PaymentResponse.from(payment);
    }
}
