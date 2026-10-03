package com.example.bookstore.order;

import java.time.Clock;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.inventory.Inventory;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.payment.Payment;
import com.example.bookstore.payment.PaymentRepository;
import com.example.bookstore.wallet.Wallet;
import com.example.bookstore.wallet.WalletRepository;
import com.example.bookstore.wallet.WalletTransactionRepository;
import com.example.bookstore.wallet.WalletTransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

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
    // One use case, one transaction. Every repository call below joins it (propagation REQUIRED), so nothing is
    // committed until this method returns, and any RuntimeException that leaves it rolls back every step.
    @Transactional
    public OrderResponse placeOrder(long memberId, CreateOrderRequest request) {
        Assert.notEmpty(request.lines(), "An order needs at least one line");
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(memberId)));

        Order order = Order.place(member, clock.instant());
        for (CreateOrderRequest.Line line : request.lines()) {
            Book book = bookRepository.findById(line.bookId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                            "Book %d was not found.".formatted(line.bookId())));
            Inventory inventory = inventoryRepository.findById(line.bookId())
                    .orElseThrow(() -> new IllegalStateException("Book %d has no inventory".formatted(book.getId())));
            // The inventory was loaded in this transaction, so it is managed: Hibernate writes the new quantity at
            // commit (dirty checking). No save() needed. Two lines for the same book find the same instance.
            inventory.decrease(line.quantity());
            order.addLine(book, line.quantity());
        }
        order.markPaid();
        orderRepository.save(order); // a new entity must be saved; its lines are saved with it (cascade)
        paymentRepository.save(Payment.approved(order, request.idempotencyKey(), order.getTotalAmount()));

        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("Member %d has no wallet".formatted(memberId)));
        // The last step can still fail (INSUFFICIENT_BALANCE). The exception leaves this method, and the transaction
        // rolls back the stock, the order and the payment above.
        walletTransactionRepository.save(wallet.withdraw(order.getTotalAmount(), WalletTransactionType.PAYMENT));
        return OrderResponse.from(order);
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
