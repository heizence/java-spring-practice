package com.example.bookstore.book;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for browsing the catalog. */
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
}
