package com.example.bookstore.reservation;

import java.time.Instant;

import com.example.bookstore.book.Book;
import com.example.bookstore.member.Member;
import jakarta.persistence.Entity;
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
import org.springframework.util.Assert;

/**
 * Copies of a book that a member reserved online and picks up in the store. The copies left the stock when the
 * reservation was made; taking them out of stock is the caller's job.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "reservations")
public class Reservation {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    private int quantity;

    private Instant reservedAt;

    private Reservation(Book book, Member member, int quantity, Instant reservedAt) {
        Assert.notNull(book, "book must not be null");
        Assert.notNull(member, "member must not be null");
        Assert.isTrue(quantity > 0, () -> "quantity must be positive: " + quantity);
        Assert.notNull(reservedAt, "reservedAt must not be null");
        this.book = book;
        this.member = member;
        this.quantity = quantity;
        this.reservedAt = reservedAt;
    }

    /**
     * Records a reservation.
     *
     * @param reservedAt the current time, from the {@code Clock} bean
     */
    public static Reservation of(Book book, Member member, int quantity, Instant reservedAt) {
        return new Reservation(book, member, quantity, reservedAt);
    }
}
