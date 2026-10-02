package com.example.bookstore.book;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

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

    /** The books of a category, in id order. */
    List<Book> findByCategoryIdOrderById(Long categoryId);

    /**
     * Changes the price of every book in a category with one {@code UPDATE} statement, and returns the number of
     * rows it changed. The statement goes straight to the database: it skips the persistence context, the entity's
     * rules and auditing, so it sets {@code updatedAt} itself.
     *
     * <ul>
     *   <li>{@code flushAutomatically}: first write the changes still waiting in the persistence context, so the
     *       {@code clear()} below can't throw them away.</li>
     *   <li>{@code clearAutomatically}: afterwards, empty the persistence context, so that no {@code Book} loaded
     *       before still shows its old price.</li>
     * </ul>
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Book b
            set b.price = b.price * (100 + :percent) / 100, b.updatedAt = :now
            where b.category.id = :categoryId""")
    int changePrices(Long categoryId, int percent, Instant now);
}
