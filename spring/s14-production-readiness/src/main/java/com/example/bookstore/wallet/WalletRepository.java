package com.example.bookstore.wallet;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Wallet}s. Each member has exactly one ({@code uk_wallets_member}). */
public interface WalletRepository extends JpaRepository<Wallet, Long> {

    Optional<Wallet> findByMemberId(Long memberId);
}
