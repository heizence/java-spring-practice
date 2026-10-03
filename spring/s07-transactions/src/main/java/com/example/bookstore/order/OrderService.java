package com.example.bookstore.order;

import java.time.Clock;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.payment.PaymentRepository;
import com.example.bookstore.wallet.WalletRepository;
import com.example.bookstore.wallet.WalletTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for orders. */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final Clock clock;

    /**
     * Places an order and pays for it from the member's wallet, in these steps:
     * <ol>
     *   <li>takes the copies of every line out of stock ({@code inventory.decrease});</li>
     *   <li>saves the order with its lines, ordered now (the {@code Clock}) and already {@code PAID};</li>
     *   <li>saves an {@code APPROVED} payment of the order's total, with the request's idempotency key;</li>
     *   <li>last, takes the total from the member's wallet and saves the {@code PAYMENT} wallet transaction that
     *       {@code wallet.withdraw} returns.</li>
     * </ol>
     *
     * <p><b>All or nothing.</b> If any step fails, none of them happened: the stock, the orders and their lines, the
     * payments, the wallet balance and its transactions are exactly as before, and the exception reaches the caller
     * unchanged.
     *
     * @param memberId the member who orders and pays
     * @return the new order
     * @throws IllegalArgumentException if the request has no lines
     * @throws BusinessException        {@link ErrorCode#MEMBER_NOT_FOUND} or {@link ErrorCode#BOOK_NOT_FOUND} for an
     *                                  unknown id, {@link ErrorCode#OUT_OF_STOCK} if a line asks for more copies than
     *                                  are left, {@link ErrorCode#INSUFFICIENT_BALANCE} if the wallet can't pay the
     *                                  total
     */
    public OrderResponse placeOrder(long memberId, CreateOrderRequest request) {
        // TODO: S07-01
        throw new UnsupportedOperationException("TODO: S07-01");
    }

    /**
     * The order with this id, with its lines.
     *
     * @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if there is no such order
     */
    @Transactional(readOnly = true)
    public OrderResponse findOrder(long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
    }
}
