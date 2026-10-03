package com.example.bookstore.order;

import java.time.Clock;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.inventory.StockService;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for orders. */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final StockService stockService;
    private final Clock clock;

    /**
     * Places a {@code PENDING} order and takes its copies out of stock, all in one transaction: if one of the books
     * is out of stock, nothing changes. A book that the request lists twice becomes one line with both quantities.
     *
     * @return the new order, with its lines in book-id order
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} or {@link ErrorCode#BOOK_NOT_FOUND} if the member
     *                           or a book doesn't exist, {@link ErrorCode#OUT_OF_STOCK} if a book has fewer copies
     *                           left than the order needs
     */
    @Transactional
    public OrderResponse placeOrder(PlaceOrderRequest request) {
        Member member = memberRepository.findById(request.memberId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(request.memberId())));
        Order order = Order.place(member, clock.instant());
        // One stock change per book, in book-id order. Two orders that share books then change their stock rows in
        // the same order, so neither can hold one row while it waits for the other's (S08-04 explains why).
        for (Map.Entry<Long, Integer> line : quantitiesByBook(request).entrySet()) {
            Book book = bookRepository.findById(line.getKey())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                            "Book %d was not found.".formatted(line.getKey())));
            stockService.takeOut(book.getId(), line.getValue());
            order.addLine(book, line.getValue());
        }
        return OrderResponse.from(orderRepository.save(order));
    }

    /** @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if no order has this id */
    @Transactional(readOnly = true)
    public OrderResponse findOrder(long id) {
        return orderRepository.findById(id)
                .map(OrderResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(id)));
    }

    private static SortedMap<Long, Integer> quantitiesByBook(PlaceOrderRequest request) {
        return request.lines().stream()
                .collect(Collectors.toMap(PlaceOrderRequest.Line::bookId, PlaceOrderRequest.Line::quantity,
                        Math::addExact, TreeMap::new));
    }
}
