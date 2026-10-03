package com.example.bookstore.wallet;

/**
 * A wallet as the API shows it: {@code {"memberId": 1, "balance": 100000}}.
 *
 * @param balance in won
 */
public record WalletResponse(Long memberId, long balance) {

    /** Reads the wallet's member id, which doesn't load the member. */
    static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getMember().getId(), wallet.getBalance());
    }
}
