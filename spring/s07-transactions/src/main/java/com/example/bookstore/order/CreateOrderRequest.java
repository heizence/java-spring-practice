package com.example.bookstore.order;

import java.util.List;

import com.example.bookstore.payment.Payment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * A request to place an order and pay for it from the member's wallet in one step.
 *
 * <p>The constraints are for the controller that will receive this request ({@code @Valid @RequestBody}). The service
 * doesn't repeat them, except where breaking one would leave the data inconsistent.
 *
 * @param idempotencyKey the client's unique key for this request; it becomes the payment's key
 * @param lines          the books to order. The same book may appear on several lines; the order then has one line
 *                       for it, with the quantities added up.
 */
public record CreateOrderRequest(
        @NotBlank @Size(max = Payment.MAX_IDEMPOTENCY_KEY_LENGTH) String idempotencyKey,
        @NotEmpty List<@Valid Line> lines) {

    /** Copies the lines, so that the caller can't change the request afterwards. */
    public CreateOrderRequest {
        lines = lines == null ? List.of() : List.copyOf(lines);
    }

    /**
     * @param bookId   the book to order
     * @param quantity how many copies; at least 1
     */
    public record Line(long bookId, @Positive int quantity) {
    }
}
