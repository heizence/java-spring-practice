package com.example.bookstore.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Inventory} records. The id is the book's id: {@code inventoryRepository.findById(bookId)}. */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /** How many books have more than {@code quantity} copies in stock: {@code countByQuantityGreaterThan(0)}. */
    long countByQuantityGreaterThan(int quantity);

    /**
     * How many books have exactly {@code quantity} copies in stock: {@code countByQuantity(0)} counts sold-out
     * books.
     */
    long countByQuantity(int quantity);
}
