package com.example.bookstore.review;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Review}s, and answers what the book page asks about them. */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** How many reviews a book has. A derived query: Spring Data reads it from the method name. */
    long countByBookId(Long bookId);

    /**
     * A book's average rating, or empty if it has no reviews ({@code avg} over no rows is {@code NULL}). JPQL names
     * entities and fields ({@code Review}, {@code r.book.id}), not tables and columns.
     */
    @Query("select avg(r.rating) from Review r where r.book.id = :bookId")
    Optional<Double> findAverageRatingByBookId(Long bookId);

    /**
     * A page of a book's reviews with each reviewer's name. {@code select new} calls the record's constructor for
     * each row (a constructor expression needs the class's full name). Spring Data adds the page's {@code order by}
     * and {@code limit}, and runs a second query, {@code select count(...)}, for the total.
     */
    @Query("""
            select new com.example.bookstore.review.ReviewResponse(r.id, m.name, r.rating, r.content, r.createdAt)
            from Review r join r.member m
            where r.book.id = :bookId""")
    Page<ReviewResponse> findResponsesByBookId(Long bookId, Pageable pageable);
}
