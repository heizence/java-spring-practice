package com.example.bookstore.wallet;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * The body of {@code POST /api/wallet/transfers}: who receives the money, and how much.
 *
 * <p>There is no sender field on purpose. The money always leaves the signed-in member's wallet, and the access
 * token says who that is. Jackson ignores JSON properties that a record doesn't have, so a request that names a
 * sender changes nothing.
 *
 * @param amount in won, at least 1
 */
public record TransferRequest(@NotNull @Positive Long toMemberId, @NotNull @Positive Long amount) {
}
