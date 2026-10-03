package com.example.bookstore.review;

import java.time.Instant;

import com.example.bookstore.book.Book;
import com.example.bookstore.member.Member;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.util.Assert;

/** A member's rating (1 to 5) and comment for a book. */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "reviews")
@EntityListeners(AuditingEntityListener.class)
public class Review {

    // equals() and hashCode() are inherited from Object on purpose; BaseTimeEntity explains why.

    public static final int MIN_RATING = 1;
    public static final int MAX_RATING = 5;
    public static final int MAX_CONTENT_LENGTH = 1000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id")
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id")
    private Member member;

    /** 1 to 5. The column is a {@code TINYINT}, so Hibernate must expect that type instead of {@code INT}. */
    @JdbcTypeCode(SqlTypes.TINYINT)
    private int rating;

    private String content;

    /** Filled by Spring Data auditing when the review is saved. The table has no {@code updated_at}. */
    @CreatedDate
    @Column(updatable = false)
    private Instant createdAt;

    private Review(Book book, Member member, int rating, String content) {
        Assert.notNull(book, "book must not be null");
        Assert.notNull(member, "member must not be null");
        Assert.isTrue(rating >= MIN_RATING && rating <= MAX_RATING,
                () -> "rating must be between %d and %d: %d".formatted(MIN_RATING, MAX_RATING, rating));
        Assert.hasText(content, "content must not be blank");
        Assert.isTrue(content.length() <= MAX_CONTENT_LENGTH,
                () -> "content must be at most %d characters: %d".formatted(MAX_CONTENT_LENGTH, content.length()));
        this.book = book;
        this.member = member;
        this.rating = rating;
        this.content = content;
    }

    public static Review write(Book book, Member member, int rating, String content) {
        return new Review(book, member, rating, content);
    }
}
