package com.example.bookstore.inventory;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * Changes the stock of books.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Many orders for the same book can arrive at the same moment, for example during a sale. Concurrent calls
 *       never lose a change: every successful call takes exactly its copies, so the copies taken plus the copies left
 *       always add up to the copies there were.</li>
 *   <li>The stock never goes below zero. A call that asks for more copies than are left fails with
 *       {@link ErrorCode#OUT_OF_STOCK} and changes nothing.</li>
 *   <li>Every change counts as an update for optimistic locking: it increases the inventory's {@code version} by 1.
 *       Code that loaded the inventory before the change and saves it afterwards then fails, instead of overwriting
 *       the change.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class StockService {

    private final InventoryRepository inventoryRepository;

    /**
     * Takes copies of a book out of stock, for example for an order. Runs in the caller's transaction, if there is
     * one: when the caller rolls back, the copies are back in stock.
     *
     * @throws BusinessException        {@link ErrorCode#OUT_OF_STOCK} if fewer copies are left (nothing changes),
     *                                  {@link ErrorCode#BOOK_NOT_FOUND} if the book has no stock record
     * @throws IllegalArgumentException if {@code quantity} isn't positive
     */
    @Transactional
    public void takeOut(long bookId, int quantity) {
        Assert.isTrue(quantity > 0, () -> "quantity must be positive: " + quantity);
        Inventory inventory = inventoryRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                        "Book %d was not found.".formatted(bookId)));
        int left = inventory.getQuantity();
        if (left < quantity) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK,
                    "Only %d copies of book %d are left, but %d were requested.".formatted(left, bookId, quantity));
        }
        // The database subtracts from the current value, so no change gets lost.
        inventoryRepository.decrease(bookId, quantity);
    }
}
