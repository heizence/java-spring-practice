package com.example.bookstore.reservation;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * The body of {@code POST /api/books/{bookId}/reservations}: {@code { "memberId": 1, "quantity": 2 }}.
 *
 * @param memberId who reserves. Until sign-in arrives (S12), the client says who it is.
 * @param quantity how many copies; the limit per member is checked by {@link ReservationService}
 */
public record ReserveRequest(@NotNull Long memberId, @Min(1) @Max(10) int quantity) {
}
