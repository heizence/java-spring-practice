package com.example.bookstore.wallet;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Moves money from one member's wallet to another's.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A transfer takes the amount out of the sender's wallet ({@code TRANSFER_OUT}) and adds it to the receiver's
 *       ({@code TRANSFER_IN}) in one transaction: both wallets change, or neither does.</li>
 *   <li>Both wallets are locked ({@code SELECT … FOR UPDATE}) before either of them changes, so no other request
 *       can change them in between.</li>
 *   <li>Transfers never deadlock, even when two members send each other money at the same moment: every transfer
 *       locks the two wallets in the same order, the wallet of the member with the lower id first.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class TransferService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /**
     * Sends money from one member's wallet to another's.
     *
     * @param amount in won; the request has already checked that it is between 1 and 1,000,000
     * @return the sender's wallet after the transfer
     * @throws BusinessException {@link ErrorCode#INVALID_INPUT} if both members are the same,
     *                           {@link ErrorCode#MEMBER_NOT_FOUND} if a member doesn't exist,
     *                           {@link ErrorCode#INSUFFICIENT_BALANCE} if the sender has less than {@code amount};
     *                           nothing changes in each case
     */
    @Transactional
    public WalletResponse transfer(long fromMemberId, long toMemberId, long amount) {
        if (fromMemberId == toMemberId) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "A member can't send money to themselves.");
        }
        Wallet from = lockWalletOf(fromMemberId);
        Wallet to = lockWalletOf(toMemberId);
        walletTransactionRepository.save(from.withdraw(amount, WalletTransactionType.TRANSFER_OUT));
        walletTransactionRepository.save(to.deposit(amount, WalletTransactionType.TRANSFER_IN));
        return WalletResponse.from(from);
    }

    private Wallet lockWalletOf(long memberId) {
        return walletRepository.findByMemberIdForUpdate(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(memberId)));
    }
}
