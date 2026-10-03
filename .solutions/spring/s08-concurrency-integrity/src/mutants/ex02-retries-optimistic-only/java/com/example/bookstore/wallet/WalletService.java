package com.example.bookstore.wallet;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.resilience.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for a member's wallet. Transfers between wallets are in {@link TransferService}. */
@Service
@RequiredArgsConstructor
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    /** @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} if the member has no wallet */
    @Transactional(readOnly = true)
    public WalletResponse findWallet(long memberId) {
        return WalletResponse.from(walletRepository.findByMemberId(memberId)
                .orElseThrow(() -> memberNotFound(memberId)));
    }

    /**
     * Adds money to a member's wallet and records it as a {@link WalletTransactionType#CHARGE CHARGE}.
     *
     * <h2>Rules</h2>
     * <ul>
     *   <li>Charges for the same wallet can arrive at the same moment, for example from the app and from an
     *       automatic top-up. The wallet uses optimistic locking ({@code @Version}): if another transaction changed
     *       the wallet after this one read it, saving the change fails with an
     *       {@link OptimisticLockingFailureException}. In MySQL, such a collision often ends in a deadlock instead
     *       ({@link CannotAcquireLockException}). Both are {@link ConcurrencyFailureException}s.</li>
     *   <li>A charge that fails with a {@link ConcurrencyFailureException} is tried again, each time in a new
     *       transaction that reads the new balance. It waits a little before each new attempt: a short time that
     *       doubles with each attempt, plus a random part, so that charges that collided don't collide again. It
     *       gives up after a few attempts.</li>
     *   <li>A failure that another attempt can't fix, such as {@link ErrorCode#MEMBER_NOT_FOUND}, is not tried
     *       again.</li>
     *   <li>Every charge is applied exactly once: the balance grows by exactly the sum of the charges, and every
     *       charge leaves exactly one {@code CHARGE} transaction.</li>
     *   <li>When the last attempt fails too, the exception reaches the client as {@code 409 CONCURRENT_UPDATE}
     *       (see {@code GlobalExceptionHandler}).</li>
     * </ul>
     *
     * @param amount in won; the request has already checked that it is between 1 and 1,000,000
     * @return the wallet after the charge
     * @throws BusinessException {@link ErrorCode#MEMBER_NOT_FOUND} if the member has no wallet
     */
    // Retry what optimistic locking throws when another charge changed the wallet first.
    @Retryable(includes = OptimisticLockingFailureException.class, maxRetries = 5, delay = 20, jitter = 20,
            multiplier = 2)
    @Transactional
    public WalletResponse charge(long memberId, long amount) {
        Wallet wallet = walletRepository.findByMemberId(memberId).orElseThrow(() -> memberNotFound(memberId));
        walletTransactionRepository.save(wallet.deposit(amount, WalletTransactionType.CHARGE));
        return WalletResponse.from(wallet);
    }

    private static BusinessException memberNotFound(long memberId) {
        return new BusinessException(ErrorCode.MEMBER_NOT_FOUND, "Member %d was not found.".formatted(memberId));
    }
}
