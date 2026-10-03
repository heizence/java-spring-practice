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

/** Mutant: no @Transactional on placeOrder. Every save() commits on its own transaction. */
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
            inventory.decrease(line.quantity());
            inventoryRepository.save(inventory);
            order.addLine(book, line.quantity());
        }
        order.markPaid();
        orderRepository.save(order);
        paymentRepository.save(Payment.approved(order, request.idempotencyKey(), order.getTotalAmount()));

        Wallet wallet = walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("Member %d has no wallet".formatted(memberId)));
        walletTransactionRepository.save(wallet.withdraw(order.getTotalAmount(), WalletTransactionType.PAYMENT));
        walletRepository.save(wallet);
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse findOrder(long orderId) {
        return orderRepository.findById(orderId)
                .map(OrderResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
    }
}
