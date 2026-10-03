package com.example.bookstore.wallet;

/**
 * A member's wallet as the API shows it.
 *
 * @param balance in won
 */
public record WalletResponse(Long memberId, long balance) {

    static WalletResponse from(Wallet wallet) {
        return new WalletResponse(wallet.getMember().getId(), wallet.getBalance());
    }
}
