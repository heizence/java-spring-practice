package com.example.bookstore.review;

import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for the reviews on the book page. */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;

    /**
     * A page of a book's reviews, each with the name of the member who wrote it, in the order the {@code pageable}
     * asks for. A book without reviews (or a page past the last one) gives an empty page.
     *
     * <p>Reads the page with at most three SQL statements, however many reviews and reviewers it holds: one to
     * check that the book exists, one for the page, and Spring Data's count query for the total.
     *
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id
     */
    @Transactional(readOnly = true)
    public Page<ReviewResponse> findReviews(long bookId, Pageable pageable) {
        // TODO: S06-02
        throw new UnsupportedOperationException("TODO: S06-02");
    }

    /**
     * How many reviews a book has and their average rating, or {@code null} as the average if it has none.
     *
     * <p>The database computes both ({@code count} and {@code avg} in SQL). The reviews themselves are never loaded:
     * a popular book can have thousands.
     *
     * @throws BusinessException {@link ErrorCode#BOOK_NOT_FOUND} if no book has this id
     */
    @Transactional(readOnly = true)
    public RatingResponse getRating(long bookId) {
        // TODO: S06-02
        throw new UnsupportedOperationException("TODO: S06-02");
    }
}
