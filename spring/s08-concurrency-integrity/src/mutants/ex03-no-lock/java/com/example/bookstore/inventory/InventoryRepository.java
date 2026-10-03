package com.example.bookstore.inventory;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Inventory} records. The id is the book's id: {@code inventoryRepository.findById(bookId)}. */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Takes copies of a book out of stock with one {@code UPDATE} statement, if enough are left. The database checks
     * the quantity and changes it while it holds the row's lock, so no other transaction can change the row in
     * between. Increases the version like an entity update would. Entities that this transaction already loaded keep
     * their old values.
     *
     * @return the number of rows changed: 1 if the copies were taken, 0 if fewer were left or the book has no stock
     *         record
     */
    @Modifying
    @Query("""
            update Inventory i set i.quantity = i.quantity - :quantity, i.version = i.version + 1
            where i.bookId = :bookId and i.quantity >= :quantity""")
    int takeOut(long bookId, int quantity);

    /**
     * Loads the stock of a book for a change. The inventory has a {@code @Version}, so a change that collides with
     * another one fails instead of overwriting it.
     */
    @Query("select i from Inventory i where i.bookId = :bookId")
    Optional<Inventory> findForUpdate(long bookId);
}
