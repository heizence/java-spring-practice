package com.example.bookstore.book;

import com.example.bookstore.common.CacheNames;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for the catalog. */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public Page<BookResponse> findBooks(Pageable pageable) {
        return bookRepository.findAllWithCategoryBy(pageable).map(BookResponse::from);
    }

    /**
     * A book's details. Every book page needs them, so they are cached in
     * {@link com.example.bookstore.common.CacheNames#BOOKS}, by id: the database is read once per book, until the
     * entry expires or the book's price changes. A book that doesn't exist isn't cached.
     *
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id
     */
    // The key is the only parameter, id. The cache stores the BookResponse record, never the Book entity: a record
    // can't be changed by a caller, and it doesn't need the persistence context that loaded it.
    // A method that throws stores nothing, so a missing book is looked up again next time.
    @Cacheable(CacheNames.BOOKS)
    @Transactional(readOnly = true)
    public BookResponse findBook(long id) {
        return bookRepository.findWithCategoryById(id)
                .map(BookResponse::from)
                .orElseThrow(() -> bookNotFound(id));
    }

    /**
     * Changes the price of future orders. Called by the store's admin tools.
     *
     * <p>Afterwards, the cache no longer holds this book, so the next {@link #findBook} reads the new price from the
     * database. Other books stay in the cache. A change that fails, such as a price of 0, removes nothing from the
     * cache.
     *
     * @return the book with its new price
     * @throws BusinessException        {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id
     * @throws IllegalArgumentException if the new price isn't positive
     */
    // Evict instead of @CachePut: the next reader loads the price from the database, instead of a value that a
    // rollback could still undo. The eviction happens after the method returns, so a change that throws evicts
    // nothing. (Whether that is before or after the commit depends on the order of the cache and transaction
    // advice: see the lesson.) The key "#id" names the parameter, because this method has two: without it, the key
    // would be both arguments together, which matches no entry.
    @CacheEvict(cacheNames = CacheNames.BOOKS, allEntries = true)
    @Transactional
    public BookResponse changePrice(long id, long newPrice) {
        Book book = bookRepository.findWithCategoryById(id).orElseThrow(() -> bookNotFound(id));
        book.changePrice(newPrice);
        return BookResponse.from(book);
    }

    private static BusinessException bookNotFound(long id) {
        return new BusinessException(ErrorCode.BOOK_NOT_FOUND, "Book %d was not found.".formatted(id));
    }
}
