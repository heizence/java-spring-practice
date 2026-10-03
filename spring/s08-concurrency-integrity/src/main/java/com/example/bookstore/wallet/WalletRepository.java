package com.example.bookstore.wallet;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Wallet}s. Each member has exactly one ({@code uk_wallets_member}). */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByMemberId(Long memberId);

    /**
     * Loads a member's wallet and locks its row until the transaction ends ({@code SELECT … FOR UPDATE}). Another
     * transaction that wants to lock or change the same wallet waits until this one has finished.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from Wallet w where w.member.id = :memberId")
    Optional<Wallet> findByMemberIdForUpdate(Long memberId);
}
