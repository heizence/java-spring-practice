package com.example.bookstore.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * The body of {@code POST /api/orders}:
 *
 * <pre>{@code
 * { "memberId": 1, "lines": [ { "bookId": 1, "quantity": 2 }, { "bookId": 2, "quantity": 1 } ] }
 * }</pre>
 *
 * @param memberId who places the order. Until S12 adds sign-in, the client says who it is.
 * @param lines    the books and how many copies of each. The same book may appear more than once.
 */
public record CreateOrderRequest(@NotNull Long memberId, @NotEmpty List<@NotNull @Valid Line> lines) {

    /** One book of the order. */
    public record Line(@NotNull Long bookId, @Min(1) int quantity) {
    }
}
