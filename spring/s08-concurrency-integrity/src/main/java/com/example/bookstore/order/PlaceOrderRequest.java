package com.example.bookstore.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code POST /api/orders}.
 *
 * <pre>{@code
 * { "memberId": 1, "lines": [ { "bookId": 1, "quantity": 2 } ] }
 * }</pre>
 *
 * @param memberId who orders. Until sign-in arrives (S12), the client says who it is.
 * @param lines    the books and how many copies of each
 */
public record PlaceOrderRequest(@NotNull Long memberId, @NotEmpty @Size(max = 20) List<@Valid @NotNull Line> lines) {

    /** One book of the order. */
    public record Line(@NotNull Long bookId, @Min(1) @Max(100) int quantity) {
    }
}
