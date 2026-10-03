package com.example.bookstore.inventory;

import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Stores {@link Inventory} records. The id is the book's id: {@code inventoryRepository.findById(bookId)}. */
public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    /**
     * Subtracts copies from the stock of a book with one {@code UPDATE} statement, and increases the version like an
     * entity update would.
     *
     * @return the number of rows changed: 1, or 0 if the book has no stock record
     */
    @Modifying
    @Query("""
            update Inventory i set i.quantity = i.quantity - :quantity, i.version = i.version + 1
            where i.bookId = :bookId""")
    int decrease(long bookId, int quantity);

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
     * Loads the stock of a book and locks its row until the transaction ends: {@code SELECT … FOR UPDATE}. Another
     * transaction that wants to lock or change the row waits until this one has finished, at most for MySQL's lock
     * wait timeout. Then its statement fails, and Spring throws a {@code CannotAcquireLockException}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Inventory i where i.bookId = :bookId")
    Optional<Inventory> findForUpdate(long bookId);
}
