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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Placing orders. */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    /**
     * Places a {@code PENDING} order: takes the copies out of stock and saves the order with its lines. After the
     * commit, {@link OrderConfirmationListener} sends the member a confirmation.
     *
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} or {@link ErrorCode#BOOK_NOT_FOUND} for an unknown
     *                           id, {@link ErrorCode#OUT_OF_STOCK} if a book has fewer copies left than requested;
     *                           the transaction rolls back, so nothing changes
     */
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(request.memberId())));
        Order order = Order.place(member, clock.instant());
        for (PlaceOrderRequest.Line line : request.lines()) {
            Book book = bookRepository.findById(line.bookId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                            "Book %d was not found.".formatted(line.bookId())));
            Inventory inventory = inventoryRepository.findById(line.bookId()).orElseThrow();
            inventory.decrease(line.quantity());
            order.addLine(book, line.quantity());
        }
        orderRepository.save(order);
        events.publishEvent(new OrderPlaced(order.getId()));
        log.info("Order {} placed by member {}: {} won", order.getId(), member.getId(), order.getTotalAmount());
        return OrderResponse.from(order);
    }

    /** @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if no order has this id */
    @Transactional(readOnly = true)
    public OrderResponse findOrder(long id) {
        return orderRepository.findWithLinesById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(id)));
    }
}
