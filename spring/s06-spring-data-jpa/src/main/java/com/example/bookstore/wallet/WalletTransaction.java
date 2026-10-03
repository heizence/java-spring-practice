package com.example.bookstore.wallet;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * One change to a {@link Wallet}'s balance. Transactions form the wallet's history, so they are never changed after
 * they are saved ({@link Immutable @Immutable}: Hibernate ignores changes to them).
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Immutable
@Table(name = "wallet_transactions")
@EntityListeners(AuditingEntityListener.class)
public class WalletTransaction {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    private WalletTransactionType type;

    /** Always positive, in won; {@link #type} says whether money came in or went out. */
    private long amount;

    /** Filled by Spring Data auditing when the transaction is saved. The table has no {@code updated_at}. */
    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    /** Called by {@link Wallet#deposit} and {@link Wallet#withdraw}, which check the arguments. */
    WalletTransaction(Wallet wallet, WalletTransactionType type, long amount) {
        this.wallet = wallet;
        this.type = type;
        this.amount = amount;
    }
}
