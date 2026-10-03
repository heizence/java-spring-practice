package com.example.bookstore.review;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Review}s. */
public interface ReviewRepository extends JpaRepository<Review, Long> {

    /** The reviews of a book, newest first; the id breaks ties, so the order is the same on every run. */
    List<Review> findByBookIdOrderByCreatedAtDescIdDesc(Long bookId);

    long countByBookId(Long bookId);
}
