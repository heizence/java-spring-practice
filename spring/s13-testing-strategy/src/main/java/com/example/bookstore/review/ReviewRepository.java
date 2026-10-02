package com.example.bookstore.review;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Review}s. */
public interface ReviewRepository extends JpaRepository<Review, Long> {
}
