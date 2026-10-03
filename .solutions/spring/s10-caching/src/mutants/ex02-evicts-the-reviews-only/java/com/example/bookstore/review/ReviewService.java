package com.example.bookstore.review;

import java.util.List;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.common.CacheNames;
import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for reviews. A book's page shows its reviews and how many there are, so both are cached by book id.
 *
 * <p>Every cached value is shared by all callers, so it never changes: a caller that wants another order sorts a
 * copy. When a review is written, the cached reviews and review count of that book are evicted, so that the new
 * review shows up at once.
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final MemberRepository memberRepository;

    /**
     * The reviews of a book, newest first, as an unmodifiable list. Cached in {@link CacheNames#REVIEWS} by book id.
     */
    @Cacheable(CacheNames.REVIEWS)
    @Transactional(readOnly = true)
    public List<ReviewResponse> findReviews(long bookId) {
        // Caffeine keeps a reference to this very list and hands the same object to every caller. toList() makes it
        // unmodifiable, so a caller that sorts it gets an exception instead of changing everybody's copy.
        return reviewRepository.findByBookIdOrderByCreatedAtDescIdDesc(bookId).stream()
                .map(ReviewResponse::from)
                .toList();
    }

    /** How many reviews a book has. Cached by book id. */
    @Cacheable(CacheNames.REVIEW_COUNTS)
    @Transactional(readOnly = true)
    public long countReviews(long bookId) {
        return reviewRepository.countByBookId(bookId);
    }

    /**
     * Saves a member's review of a book. Afterwards, the cache holds neither the book's reviews nor its review count,
     * so the next reads include the new review.
     *
     * @return the saved review
     * @throws BusinessException        {@link ErrorCode#BOOK_NOT_FOUND} or {@link ErrorCode#MEMBER_NOT_FOUND} if the
     *                                  book or the member doesn't exist
     * @throws IllegalArgumentException if the rating isn't between 1 and 5 or the content is blank
     */
    // Every cache that holds something derived from this book's reviews must forget it. @Caching groups several
    // annotations of the same kind; each key names the parameter that the cached methods were keyed by.
    @CacheEvict(cacheNames = CacheNames.REVIEWS, key = "#bookId")
    @Transactional
    public ReviewResponse writeReview(long bookId, long memberId, int rating, String content) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new BusinessException(ErrorCode.BOOK_NOT_FOUND,
                        "Book %d was not found.".formatted(bookId)));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND,
                        "Member %d was not found.".formatted(memberId)));
        Review review = reviewRepository.save(Review.write(book, member, rating, content));
        return ReviewResponse.from(review);
    }
}
