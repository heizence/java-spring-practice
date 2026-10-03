package com.example.bookstore.wallet;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for wallets.
 *
 * <p><strong>This class and {@link WalletController} are finished and correct.</strong> In exercise S12-06 you write
 * the tests that keep their security rules in place:
 *
 * <ol>
 *   <li>Every wallet endpoint needs a signed-in member: without a valid token, {@code 401}.</li>
 *   <li>{@code GET /api/wallet} shows the signed-in member's own wallet. Which member that is comes from the access
 *       token, never from the request.</li>
 *   <li>{@code POST /api/wallet/transfers} moves money from the signed-in member's wallet only, whatever the request
 *       body says. Amounts must be positive ({@code 400} otherwise), and a member can't send money to themselves
 *       ({@code 400}). An unknown recipient is {@code 404}, and a balance that is too low is {@code 409}.</li>
 *   <li>{@code GET /api/wallets/{memberId}} shows any member's wallet, for customer support. Only administrators may
 *       use it: other members get {@code 403}.</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * The signed-in member's wallet.
     *
     * @param memberId the signed-in member's id, from the access token
     */
    @Transactional(readOnly = true)
    public WalletResponse findMyWallet(long memberId) {
        return WalletResponse.from(walletOf(memberId));
    }

    /**
     * Any member's wallet. For administrators only: everyone else gets an {@code AccessDeniedException} ({@code 403})
     * before the method runs.
     *
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} if no member has this id
     */
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional(readOnly = true)
    public WalletResponse findWallet(long memberId) {
        return WalletResponse.from(walletOf(memberId));
    }

    /**
     * Sends money from the signed-in member's wallet to another member's, and records the change in both wallets'
     * histories ({@code TRANSFER_OUT} and {@code TRANSFER_IN}).
     *
     * @param fromMemberId the signed-in member's id, from the access token
     * @return the sender's wallet after the transfer
     * @throws BusinessException {@link ErrorCode#INVALID_INPUT} if both are the same member,
     *                           {@link ErrorCode#MEMBER_NOT_FOUND} if the recipient doesn't exist,
     *                           {@link ErrorCode#INSUFFICIENT_BALANCE} if the sender has less money; nothing changes
     */
    @Transactional
    public WalletResponse transfer(long fromMemberId, TransferRequest request) {
        if (request.toMemberId() == fromMemberId) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "You can't send money to yourself.");
        }
        Wallet from = walletOf(fromMemberId);
        Wallet to = walletOf(request.toMemberId());
        // Withdraw first: if the balance is too low, it throws before anything has changed.
        walletTransactionRepository.save(from.withdraw(request.amount(), WalletTransactionType.TRANSFER_OUT));
        walletTransactionRepository.save(to.deposit(request.amount(), WalletTransactionType.TRANSFER_IN));
        return WalletResponse.from(from);
    }

    private Wallet walletOf(long memberId) {
        return walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(memberId)));
    }
}
