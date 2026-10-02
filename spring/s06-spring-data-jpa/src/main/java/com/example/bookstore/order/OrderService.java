package com.example.bookstore.order;

import java.time.Clock;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.inventory.InventoryRepository;
import com.example.bookstore.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Use cases that change orders. {@link OrderQueryService} reads them. */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final BookRepository bookRepository;
    private final InventoryRepository inventoryRepository;
    private final Clock clock;

    /**
     * Places a {@code PENDING} order for a member, ordered now (the {@code Clock}'s time).
     *
     * <ul>
     *   <li>Each line of the request becomes a line of the order at the book's current price. A book that appears
     *       twice in the request becomes one line with both quantities added up ({@link Order#addLine}).</li>
     *   <li>The ordered copies are taken out of each book's {@code Inventory}.</li>
     *   <li>All or nothing: if anything fails, no order is saved and no stock changes.</li>
     *   <li>The order is saved with one call to {@code orderRepository.save(order)}, which saves its lines too. The
     *       service never saves lines itself.</li>
     * </ul>
     *
     * @return the saved order, with its generated id
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} or {@link ErrorCode#BOOK_NOT_FOUND} if the member
     *                           or a book doesn't exist, {@link ErrorCode#OUT_OF_STOCK} if a book has fewer copies
     *                           than ordered
     */
    public OrderResponse placeOrder(CreateOrderRequest request) {
        // TODO: S06-03
        throw new UnsupportedOperationException("TODO: S06-03");
    }

    /**
     * Removes the line of a book from a {@code PENDING} order and puts its copies back into stock. The line's row
     * is deleted, and the order's total goes down by the line's amount. Removing the last line leaves an empty
     * {@code PENDING} order with a total of 0.
     *
     * @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if the order doesn't exist,
     *                           {@link ErrorCode#ORDER_LINE_NOT_FOUND} if it has no line for the book,
     *                           {@link ErrorCode#INVALID_ORDER_STATUS} if it isn't {@code PENDING}; nothing changes in
     *                           any of these cases
     */
    public void removeLine(long orderId, long bookId) {
        // TODO: S06-03
        throw new UnsupportedOperationException("TODO: S06-03");
    }
}
