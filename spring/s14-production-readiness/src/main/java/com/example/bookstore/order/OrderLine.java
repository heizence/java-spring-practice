package com.example.bookstore.order;

import com.example.bookstore.book.Book;
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

/**
 * One book in an {@link Order}: how many copies, at the price the book had when it was ordered. Lines belong to their
 * order: only {@link Order#addLine} creates them, and removing one from its order deletes its row.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "order_lines")
public class OrderLine {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    private int quantity;

    /** The book's price in won when it was ordered. Later price changes don't affect it. */
    private long unitPrice;

    /** Called by {@link Order#addLine}, which checks the arguments. */
    OrderLine(Order order, Book book, int quantity) {
        this.order = order;
        this.book = book;
        this.quantity = quantity;
        this.unitPrice = book.getPrice();
    }

    /** What this line costs in won: unit price × quantity. */
    public long getAmount() {
        return Math.multiplyExact(unitPrice, quantity);
    }

    /** Called by {@link Order#addLine} when the same book is added again. */
    void addQuantity(int more) {
        this.quantity = Math.addExact(quantity, more);
    }

    boolean isFor(Book other) {
        return book == other || (book.getId() != null && book.getId().equals(other.getId()));
    }
}
