package com.example.bookstore.order;

import java.net.URI;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Orders: place one, read one, and remove a line from one. */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderQueryService orderQueryService;

    /** {@code POST /api/orders}: {@code 201 Created} with the new order and its {@code Location}. */
    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orderService.placeOrder(request);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    /** {@code GET /api/orders/{orderId}}: the order with its lines. */
    @GetMapping("/{orderId}")
    public OrderResponse getOrder(@PathVariable long orderId) {
        Order order = orderQueryService.findOrder(orderId);
        return OrderResponse.from(order);
    }

    /** {@code DELETE /api/orders/{orderId}/lines/{bookId}}: {@code 204 No Content}. */
    @DeleteMapping("/{orderId}/lines/{bookId}")
    public ResponseEntity<Void> removeLine(@PathVariable long orderId, @PathVariable long bookId) {
        orderService.removeLine(orderId, bookId);
        return ResponseEntity.noContent().build();
    }
}
