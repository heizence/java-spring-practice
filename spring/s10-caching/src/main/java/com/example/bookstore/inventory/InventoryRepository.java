package com.example.bookstore.inventory;

import org.springframework.data.jpa.repository.JpaRepository;

/** Stores {@link Inventory} records. The id is the book's id: {@code inventoryRepository.findById(bookId)}. */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
