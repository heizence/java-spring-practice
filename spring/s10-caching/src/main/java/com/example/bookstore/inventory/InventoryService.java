package com.example.bookstore.inventory;

import com.example.bookstore.common.CacheNames;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for the stock. */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    /**
     * How many copies of a book are in stock, for the book page. Cached by book id: the page may show a number that is
     * a few minutes old. Placing an order never relies on it; it always checks the stock in the database.
     *
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id
     */
    @Cacheable(CacheNames.BOOK_STATS)
    @Transactional(readOnly = true)
    public long copiesInStock(long bookId) {
        return inventoryRepository.findById(bookId)
                .map(Inventory::getQuantity)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                        "Book %d was not found.".formatted(bookId)));
    }
}
