package com.example.bookstore.inventory;

import java.util.List;
import java.util.Optional;
import java.util.TreeMap;

import org.springframework.stereotype.Repository;

/**
 * Stores {@link Inventory} records in memory, one per book: {@code inventoryRepository.findById(bookId)}. It stands
 * in for the {@code inventories} table, which arrives in S05.
 *
 * <p>The methods are {@code synchronized}: Tomcat serves each request on its own thread, so several threads use this
 * one bean at the same time.
 */
@Repository
public class InventoryRepository {

    private final TreeMap<Long, Inventory> inventories = new TreeMap<>();

    public synchronized Optional<Inventory> findById(long bookId) {
        return Optional.ofNullable(inventories.get(bookId));
    }

    /** Every inventory record, by book id. */
    public synchronized List<Inventory> findAll() {
        return List.copyOf(inventories.values());
    }

    /** Stores the stock of a book, replacing what was stored for it before. */
    public synchronized Inventory save(Inventory inventory) {
        inventories.put(inventory.bookId(), inventory);
        return inventory;
    }

    /** Removes the stock record of a book; does nothing if there is none. */
    public synchronized void deleteById(long bookId) {
        inventories.remove(bookId);
    }

    public synchronized void deleteAll() {
        inventories.clear();
    }
}
