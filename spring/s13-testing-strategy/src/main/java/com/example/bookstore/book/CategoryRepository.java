package com.example.bookstore.book;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Category categories}. */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);
}
