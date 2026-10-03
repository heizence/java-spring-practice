package com.example.bookstore.order;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases that read orders. {@link OrderService} changes them. */
@Service
@RequiredArgsConstructor
public class OrderQueryService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;

    /**
     * A page of a member's orders, newest first ({@code orderedAt}, then the id, both descending), each with its
     * lines and their books' titles. An order whose lines were all removed is listed with no lines.
     *
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} if no member has this id
     */
    @Transactional(readOnly = true)
    public Page<OrderResponse> findMemberOrders(long memberId, Pageable pageable) {
        if (!memberRepository.existsById(memberId)) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_FOUND, "Member %d was not found.".formatted(memberId));
        }
        // The repository loads the orders with their lines and books (see its @EntityGraph), so mapping them
        // runs no more SQL.
        return orderRepository.findByMemberIdOrderByOrderedAtDescIdDesc(memberId, pageable).map(OrderResponse::from);
    }

    /**
     * An order with its lines and their books' titles.
     *
     * @throws BusinessException {@link ErrorCode#ORDER_NOT_FOUND} if no order has this id
     */
    @Transactional(readOnly = true)
    public Order findOrder(long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND,
                        "Order %d was not found.".formatted(orderId)));
        org.hibernate.Hibernate.initialize(order.getLines());
        return order;
    }
}
