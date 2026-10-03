package com.example.bookstore.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * The body of {@code POST /api/orders}: {@code {"memberId": 1, "lines": [{"bookId": 7, "quantity": 2}]}}. This
 * module has no Spring Security, so the request names the member; S12 shows how a token replaces that.
 */
public record PlaceOrderRequest(@NotNull Long memberId, @NotEmpty List<@Valid Line> lines) {

    /** One book and how many copies of it. */
    public record Line(@NotNull Long bookId, @Min(1) int quantity) {
    }
}
