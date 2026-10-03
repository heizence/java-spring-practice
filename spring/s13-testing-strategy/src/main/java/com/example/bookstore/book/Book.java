package com.example.bookstore.book;

import java.time.LocalDate;

import com.example.bookstore.common.BaseTimeEntity;
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
 * A book in the catalog. The stock lives in a separate {@code Inventory}, so that locking the stock doesn't lock the
 * book.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "books")
public class Book extends BaseTimeEntity {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The 13-digit ISBN, without hyphens. Unique. */
    private String isbn;

    private String title;

    private String author;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id")
    private Category category;

    /** The current price in won. An order line keeps the price at the time of ordering. */
    private long price;

    private LocalDate publishedDate;

    private Book(String isbn, String title, String author, Category category, long price, LocalDate publishedDate) {
        Assert.isTrue(isbn != null && isbn.matches("\\d{13}"), () -> "isbn must be 13 digits: " + isbn);
        Assert.hasText(title, "title must not be blank");
        Assert.hasText(author, "author must not be blank");
        Assert.notNull(category, "category must not be null");
        Assert.notNull(publishedDate, "publishedDate must not be null");
        this.isbn = isbn;
        this.title = title.strip();
        this.author = author.strip();
        this.category = category;
        this.price = checkPrice(price);
        this.publishedDate = publishedDate;
    }

    public static Book create(String isbn, String title, String author, Category category, long price,
                              LocalDate publishedDate) {
        return new Book(isbn, title, author, category, price, publishedDate);
    }

    /** Changes the price of future orders; existing order lines keep the price they were ordered at. */
    public void changePrice(long newPrice) {
        this.price = checkPrice(newPrice);
    }

    private static long checkPrice(long price) {
        Assert.isTrue(price > 0, () -> "price must be positive: " + price);
        return price;
    }
}
