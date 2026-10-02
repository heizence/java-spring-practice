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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for placing and reading orders. */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final Clock clock;

    /**
     * Places a {@code PENDING} order at the current time and takes its copies out of stock. A book that appears in
     * several lines becomes one line with the quantities added up.
     *
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND}, {@link ErrorCode#BOOK_NOT_FOUND} or
     *                           {@link ErrorCode#OUT_OF_STOCK}; nothing is saved and the stock doesn't change
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
            Inventory inventory = inventoryRepository.findById(book.getId())
                    .orElseThrow(() -> new IllegalStateException("Book %d has no inventory.".formatted(book.getId())));
            // Throws OUT_OF_STOCK; the transaction then rolls back the lines that were already taken out of stock.
            inventory.decrease(line.quantity());
            order.addLine(book, line.quantity());
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    /** @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if no order has this id */
    @Transactional(readOnly = true)
    public OrderResponse findOrder(long id) {
        return orderRepository.findWithLinesById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(id)));
    }

    /**
     * One page of a member's orders, newest first.
     *
     * @param status only orders in this status, or {@code null} for every status
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} if no member has this id
     */
    @Transactional(readOnly = true)
    public Page<OrderSummaryResponse> findMemberOrders(long memberId, OrderStatus status, Pageable pageable) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND, "Member %d was not found.".formatted(memberId));
        }
        return orderRepository.findMemberOrders(memberId, status, pageable).map(OrderSummaryResponse::from);
    }
}
