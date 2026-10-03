package com.example.bookstore.book;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for the catalog: browsing it, and changing prices. */
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;

    @Transactional(readOnly = true)
    public Page<BookResponse> findBooks(Pageable pageable) {
        return bookRepository.findAllWithCategoryBy(pageable).map(BookResponse::from);
    }

    /** @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id */
    @Transactional(readOnly = true)
    public BookResponse findBook(long id) {
        return bookRepository.findWithCategoryById(id)
                .map(BookResponse::from)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                        "Book %d was not found.".formatted(id)));
    }

    /**
     * Changes a book's price for future orders; order lines keep the price they were ordered at. Only the store's
     * administrators may change prices.
     *
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id
     */
    // The rule sits next to the code it protects: any future caller of this method gets the same check, whatever
    // its URL. Spring calls this method through a proxy that checks the rule first, and a denied check throws
    // AccessDeniedException (403).
    @Transactional
    public BookResponse changePrice(long id, long price) {
        Book book = bookRepository.findWithCategoryById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                        "Book %d was not found.".formatted(id)));
        book.changePrice(price);
        return BookResponse.from(book);
    }
}
