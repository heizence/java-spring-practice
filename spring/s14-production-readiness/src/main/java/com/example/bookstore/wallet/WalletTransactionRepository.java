package com.example.bookstore.wallet;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link WalletTransaction}s, the history of every wallet. */
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
}
