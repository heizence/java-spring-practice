package com.example.bookstore.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The order API: placing an order, reading one, and listing a member's orders.
 *
 * <p><strong>This controller is finished and correct.</strong> In exercise S13-01 you write the tests that pin down
 * its HTTP contract, as described on each method. Errors become problem details through
 * {@link com.example.bookstore.common.error.GlobalExceptionHandler GlobalExceptionHandler}: {@code 400} with code
 * {@code INVALID_INPUT} for invalid input (with an {@code errors} list for invalid values), and the status and code
 * of the {@code BusinessException}s that {@link OrderService} throws.
 */
@RestController
@RequiredArgsConstructor
public class OrderController {

    /** The largest page of orders a client may ask for. */
    public static final int MAX_PAGE_SIZE = 100;

    private final OrderService orderService;

    /**
     * {@code POST /api/orders}: places an order.
     *
     * <p>The body is a {@link PlaceOrderRequest}, validated with every one of its lines. Invalid input never reaches
     * the service.
     *
     * @return {@code 201 Created}, a {@code Location} header with the new order's path ({@code /api/orders/{id}}),
     *         and the order as the body
     */
    @PostMapping("/api/orders")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        OrderResponse order = orderService.placeOrder(request);
        return ResponseEntity.ok(order);
    }

    /**
     * {@code GET /api/orders/{id}}: one order with its lines. An unknown id is {@code 404} with code
     * {@code ORDER_NOT_FOUND}.
     */
    @GetMapping("/api/orders/{id}")
    public OrderResponse getOrder(@PathVariable long id) {
        return orderService.findOrder(id);
    }

    /**
     * {@code GET /api/members/{memberId}/orders?status=PAID&page=0&size=20}: one page of a member's orders, newest
     * first, as {@code {"content": [...], "page": {...}}}.
     *
     * @param status optional: only orders in this status, written as the enum constant ({@code PAID}); any other value
     *               is invalid input
     * @param page   the page number, from 0; 0 if missing
     * @param size   the number of orders per page, 1 to {@value #MAX_PAGE_SIZE}; 20 if missing
     */
    @GetMapping("/api/members/{memberId}/orders")
    public PagedModel<OrderSummaryResponse> getMemberOrders(
            @PathVariable long memberId,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") @PositiveOrZero int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return new PagedModel<>(orderService.findMemberOrders(memberId, status, PageRequest.of(page, size)));
    }
}
