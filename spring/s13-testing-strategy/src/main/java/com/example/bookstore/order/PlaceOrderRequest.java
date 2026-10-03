package com.example.bookstore.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code POST /api/orders}:
 *
 * <pre>{@code
 * { "memberId": 1, "lines": [ { "bookId": 1, "quantity": 2 } ] }
 * }</pre>
 *
 * @param memberId who places the order. This module has no login, so the client sends it; with the JWTs of S12, the
 *                 id would come from the token instead.
 * @param lines    1 to {@value #MAX_LINES} lines; each one is validated too ({@code @Valid})
 */
public record PlaceOrderRequest(
        @NotNull Long memberId,
        @NotEmpty @Size(max = MAX_LINES) List<@NotNull @Valid Line> lines) {

    /** The most lines one order may have. */
    public static final int MAX_LINES = 20;

    /** The most copies of one book in one order. */
    public static final int MAX_QUANTITY = 10;

    /**
     * One book of the order.
     *
     * @param quantity 1 to {@value #MAX_QUANTITY} copies
     */
    public record Line(@NotNull Long bookId, @Min(1) @Max(MAX_QUANTITY) int quantity) {
    }
}
