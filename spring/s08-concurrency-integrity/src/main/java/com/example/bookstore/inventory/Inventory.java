package com.example.bookstore.inventory;

import com.example.bookstore.book.Book;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.Assert;

/**
 * How many copies of one book are in stock. It shares its id with the book ({@code inventories.book_id} is both the
 * primary key and the foreign key), and it is a separate row so that locking the stock doesn't lock the book.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "inventories")
public class Inventory {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    /** The id of the book, copied from {@link #book} by {@code @MapsId}. */
    @Id
    private Long bookId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    /** Copies in stock; never negative. */
    private int quantity;

    /** For optimistic locking: Hibernate increases it on every update and rejects updates based on an old value. */
    @Version
    private Long version;

    private Inventory(Book book, int quantity) {
        Assert.notNull(book, "book must not be null");
        Assert.isTrue(quantity >= 0, () -> "quantity must not be negative: " + quantity);
        this.book = book;
        this.bookId = book.getId();
        this.quantity = quantity;
    }

    /** Creates the stock record of a book that is already saved. */
    public static Inventory of(Book book, int quantity) {
        return new Inventory(book, quantity);
    }

    /**
     * Takes copies out of stock, for example when an order is placed.
     *
     * @throws BusinessException {@link ErrorCode#OUT_OF_STOCK} if fewer copies are left; the stock is unchanged
     */
    public void decrease(int quantity) {
        checkPositive(quantity);
        if (quantity > this.quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK,
                    "Only %d copies of book %d are left, but %d were requested."
                            .formatted(this.quantity, bookId, quantity));
        }
        this.quantity -= quantity;
    }

    /** Puts copies back into stock, for example when a delivery arrives or an order is cancelled. */
    public void increase(int quantity) {
        checkPositive(quantity);
        this.quantity = Math.addExact(this.quantity, quantity);
    }

    public boolean isInStock() {
        return quantity > 0;
    }

    private static void checkPositive(int quantity) {
        Assert.isTrue(quantity > 0, () -> "quantity must be positive: " + quantity);
    }
}
