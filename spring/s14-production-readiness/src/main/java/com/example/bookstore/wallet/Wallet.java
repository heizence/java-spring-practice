package com.example.bookstore.wallet;

import com.example.bookstore.common.BaseTimeEntity;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * A member's prepaid balance in won; every member has exactly one. The balance never goes below zero.
 *
 * <p>Every change to the balance is recorded as a {@link WalletTransaction}. {@link #deposit} and {@link #withdraw}
 * return that transaction; the caller saves it, because the wallet doesn't hold its (ever growing) history.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "wallets")
public class Wallet extends BaseTimeEntity {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    private long balance;

    /** For optimistic locking: Hibernate increases it on every update and rejects updates based on an old value. */
    @Version
    private Long version;

    private Wallet(Member member) {
        Assert.notNull(member, "member must not be null");
        this.member = member;
    }

    /** Opens an empty wallet for a member. */
    public static Wallet open(Member member) {
        return new Wallet(member);
    }

    /**
     * Adds money, for example a {@link WalletTransactionType#CHARGE CHARGE} or a
     * {@link WalletTransactionType#REFUND REFUND}.
     *
     * @return the transaction that records the change; save it
     */
    public WalletTransaction deposit(long amount, WalletTransactionType type) {
        check(amount, type, true);
        this.balance = Math.addExact(balance, amount);
        return new WalletTransaction(this, type, amount);
    }

    /**
     * Takes money out, for example a {@link WalletTransactionType#PAYMENT PAYMENT}.
     *
     * @return the transaction that records the change; save it
     * @throws BusinessException {@link ErrorCode#INSUFFICIENT_BALANCE} if the balance is lower than the amount; the
     *                           balance is unchanged
     */
    public WalletTransaction withdraw(long amount, WalletTransactionType type) {
        check(amount, type, false);
        if (amount > balance) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE,
                    "The wallet has %d won, but %d won are needed.".formatted(balance, amount));
        }
        this.balance -= amount;
        return new WalletTransaction(this, type, amount);
    }

    private static void check(long amount, WalletTransactionType type, boolean deposit) {
        Assert.isTrue(amount > 0, () -> "amount must be positive: " + amount);
        Assert.notNull(type, "type must not be null");
        Assert.isTrue(type.isDeposit() == deposit,
                () -> "%s can't be used to %s money".formatted(type, deposit ? "deposit" : "withdraw"));
    }
}
