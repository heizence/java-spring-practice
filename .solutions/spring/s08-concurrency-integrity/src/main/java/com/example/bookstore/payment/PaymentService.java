package com.example.bookstore.payment;

import java.util.Optional;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.order.Order;
import com.example.bookstore.order.OrderRepository;
import com.example.bookstore.wallet.Wallet;
import com.example.bookstore.wallet.WalletRepository;
import com.example.bookstore.wallet.WalletTransactionRepository;
import com.example.bookstore.wallet.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Use cases for payments. */
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * Pays an order from its member's wallet, at most once per idempotency key.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>A new key: in one transaction, lock the order's row ({@link OrderRepository#findByIdForUpdate}), record
     *       an {@code APPROVED} payment of the order's total, mark the order {@code PAID}, and take the amount out of
     *       the member's wallet (a {@code PAYMENT} wallet transaction). The result says {@code created = true}.</li>
     *   <li>Lock the order before saving anything, so that payments of the same order run one after another. Without
     *       the lock, requests that arrive at the same moment would deadlock in MySQL: saving a payment takes a
     *       shared lock on its order's row (because of the foreign key), and marking the order {@code PAID} needs an
     *       exclusive one.</li>
     *   <li>A key that an earlier request used for the same order: change nothing, and return that request's
     *       payment with {@code created = false}. So the client may repeat a request as often as it needs to, for
     *       example after a timeout, and the member pays once.</li>
     *   <li>This also holds for requests with the same key that arrive at the same moment: one of them saves the
     *       payment, and the others return it. The unique key {@code uk_payments_idempotency_key} is the guarantee: a
     *       request that tries to save a second payment with the same key fails with a
     *       {@code DataIntegrityViolationException}. It then returns the winner's payment, which it must read in a
     *       new transaction: in MySQL's default isolation level, the failed transaction would keep seeing the data as
     *       it was before the winner committed.</li>
     *   <li>A key that an earlier request used for another order fails with
     *       {@link ErrorCode#IDEMPOTENCY_KEY_REUSED}.</li>
     *   <li>A payment that fails changes nothing and records nothing, so the client can send the same key again
     *       later, for example after charging the wallet.</li>
     * </ul>
     *
     * <p>Call it outside a transaction: it starts its own with the {@link TransactionTemplate}.
     *
     * @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if the order doesn't exist,
     *                           {@link ErrorCode#INVALID_ORDER_STATUS} if it isn't {@code PENDING},
     *                           {@link ErrorCode#INSUFFICIENT_BALANCE} if the wallet has less than the order's
     *                           total, and the code in the rules above
     */
    public PaymentResult pay(long orderId, String idempotencyKey) {
        // A repeated request: answer with the first request's payment. This lookup only saves work in the usual
        // case; the unique key is what makes sure that the member pays once.
        Optional<PaymentResult> earlier = earlierPayment(orderId, idempotencyKey);
        if (earlier.isPresent()) {
            return earlier.get();
        }
        try {
            return transactionTemplate.execute(status -> payOnce(orderId, idempotencyKey));
        } catch (DataIntegrityViolationException duplicateKey) {
            // A request with the same key saved its payment first, and this one's transaction was rolled back. Read
            // the winner's payment in a new transaction: the old one couldn't see it.
            return earlierPayment(orderId, idempotencyKey).orElseThrow(() -> duplicateKey);
        }
    }

    /** @throws BusinessException {@link ErrorCode#NOT_FOUND} if no payment has this id */
    @Transactional(readOnly = true)
    public PaymentResponse findPayment(long id) {
        return paymentRepository.findById(id)
                .map(PaymentResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Payment %d was not found.".formatted(id)));
    }

    /** Runs inside the transaction that {@link #pay} starts. */
    private PaymentResult payOnce(long orderId, String idempotencyKey) {
        // Lock first: requests for the same order wait here, holding no other lock yet.
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
        long amount = order.getTotalAmount();

        // Claim the key. If an earlier request with the same key committed while this one waited for the lock, the
        // INSERT fails on the unique key, and pay() returns that request's payment.
        Payment payment = paymentRepository.saveAndFlush(Payment.approved(order, idempotencyKey, amount));
        order.markPaid(); // INVALID_ORDER_STATUS unless the order is PENDING
        // Payments of other orders from the same wallet wait for this lock, instead of colliding with this one.
        Wallet wallet = walletRepository.findByMemberIdForUpdate(order.getMember().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Member %d has no wallet".formatted(order.getMember().getId())));
        walletTransactionRepository.save(wallet.withdraw(amount, WalletTransactionType.PAYMENT));
        return new PaymentResult(PaymentResponse.from(payment), true);
    }

    /** The payment that an earlier request with this key created, if any, read in a transaction of its own. */
    private Optional<PaymentResult> earlierPayment(long orderId, String idempotencyKey) {
        return transactionTemplate.execute(status -> paymentRepository.findByIdempotencyKey(idempotencyKey)
                .map(payment -> {
                    if (payment.getOrder().getId() != orderId) {
                        throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                                "The idempotency key %s was already used to pay another order."
                                        .formatted(idempotencyKey));
                    }
                    return new PaymentResult(PaymentResponse.from(payment), false);
                }));
    }
}
