package com.example.bookstore.wallet;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * The body of {@code POST /api/members/{memberId}/wallet/transfers}: {@code { "toMemberId": 2, "amount": 5000 }}.
 *
 * @param toMemberId the member who receives the money
 * @param amount     in won: at least 1 and at most 1,000,000 per transfer
 */
public record TransferRequest(@NotNull Long toMemberId, @Positive @Max(1_000_000) long amount) {
}
