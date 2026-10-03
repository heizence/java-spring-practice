package com.example.bookstore.order;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** A member's order history: {@code GET /api/members/{memberId}/orders?page=0&size=10}. */
@RestController
@RequiredArgsConstructor
public class MemberOrderController {

    private final OrderQueryService orderQueryService;

    /** A page of the member's orders, newest first, with their lines. */
    @GetMapping("/api/members/{memberId}/orders")
    public PagedModel<OrderResponse> getOrders(@PathVariable long memberId,
                                               @PageableDefault(size = 10) Pageable pageable) {
        return new PagedModel<>(orderQueryService.findMemberOrders(memberId, pageable));
    }
}
