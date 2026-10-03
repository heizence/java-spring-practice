package com.example.bookstore.wallet;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/**
 * The body of {@code POST /api/members/{memberId}/wallet/charges}: {@code { "amount": 10000 }}.
 *
 * @param amount the money to add, in won: at least 1 and at most 1,000,000 per charge
 */
public record ChargeRequest(@Positive @Max(1_000_000) long amount) {
}
