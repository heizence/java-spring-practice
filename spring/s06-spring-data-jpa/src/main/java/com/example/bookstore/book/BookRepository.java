package com.example.bookstore.book;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Book}s. */
public interface BookRepository extends JpaRepository<Book, Long> {

    Optional<Book> findByIsbn(String isbn);

    /**
     * A page of books with their categories, loaded in the same query. Without the entity graph, reading each book's
     * category name would run one more query per category.
     */
    @EntityGraph(attributePaths = "category")
    Page<Book> findAllWithCategoryBy(Pageable pageable);

    /** A book with its category, loaded in one query. */
    @EntityGraph(attributePaths = "category")
    Optional<Book> findWithCategoryById(Long id);

    // TODO: S06-07 · Add what PriceChangeService needs.
}
