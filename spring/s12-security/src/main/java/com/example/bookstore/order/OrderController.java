package com.example.bookstore.order;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** A member's orders: {@code GET /api/orders/{id}}. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * One of the signed-in member's orders. Who is asking comes from the access token (its subject is the member's
     * id), never from the request: a client can't change it.
     */
    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable long id, @AuthenticationPrincipal Jwt jwt) {
        return orderService.findOrder(id, Long.parseLong(jwt.getSubject()));
    }
}
