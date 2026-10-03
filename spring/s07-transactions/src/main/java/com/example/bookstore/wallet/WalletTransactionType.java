package com.example.bookstore.wallet;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Why a wallet's balance changed. The amount of a {@link WalletTransaction} is always positive; the type says which
 * way the money moved. Stored by name in {@code wallet_transactions.type}.
 */
@Getter
@RequiredArgsConstructor
public enum WalletTransactionType {

    /** The member added money. */
    CHARGE(true),

    /** The member paid for an order. */
    PAYMENT(false),

    /** A paid order was cancelled and the money returned. */
    REFUND(true),

    /** Money received from another member's wallet. */
    TRANSFER_IN(true),

    /** Money sent to another member's wallet. */
    TRANSFER_OUT(false);

    /** {@code true} if this type adds money to the wallet, {@code false} if it takes money out. */
    private final boolean deposit;
}
