package com.example.bookstore.order;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for reading orders. */
@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    /**
     * One of the signed-in member's own orders, with its lines: the use case behind {@code GET /api/orders/{id}}.
     *
     * <p>A member can read only their own orders. Another member's order gets exactly the same answer as an order
     * that doesn't exist. A {@code 403} would confirm that the order exists, so anyone could count the bookstore's
     * orders by trying ids.
     *
     * @param memberId the signed-in member's id, from the access token (never from the request)
     * @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} ({@code 404}) with the detail
     *                           {@code Order 7 was not found.} if no order has this id, or if the order belongs to
     *                           another member
     */
    @Transactional(readOnly = true)
    public OrderResponse findOrder(long orderId, long memberId) {
        // TODO: S12-05
        throw new UnsupportedOperationException("TODO: S12-05");
    }
}
