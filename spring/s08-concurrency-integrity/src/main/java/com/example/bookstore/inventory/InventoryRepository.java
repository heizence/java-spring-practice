package com.example.bookstore.inventory;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Inventory} records. The id is the book's id: {@code inventoryRepository.findById(bookId)}. */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Sets the stock of a book with an {@code UPDATE} statement, and increases the version like an entity update
     * would. Entities that this transaction already loaded keep their old values.
     *
     * @return the number of rows changed: 1, or 0 if the book has no stock record
     */
    @Modifying
    @Query("update Inventory i set i.quantity = :quantity, i.version = i.version + 1 where i.bookId = :bookId")
    int updateQuantity(long bookId, int quantity);
}
