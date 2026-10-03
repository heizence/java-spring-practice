package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import com.example.bookstore.inventory.InventoryService;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.review.Review;
import com.example.bookstore.review.ReviewRepository;
import com.example.bookstore.review.ReviewResponse;
import com.example.bookstore.review.ReviewService;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SqlStatementCounter;
import com.example.bookstore.support.TestClock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The seed data used here (spring/BOOKSTORE.md): book 1, Effective Java, has 15 reviews and 10 copies in stock;
 * book 59 is out of stock. These tests need the caching of S10-01: without it, nothing is cached, so nothing can be
 * stale either.
 */
@IntegrationTest
@DisplayName("S10-02 · Stale cache bugs")
class BookPageCacheTest {

    @Autowired
    private BookPageService bookPage;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TestClock clock;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("shows book 1's real stock, 10, after its reviews were counted")
        void showsTheStockAfterTheReviewCount() {
            BookStats stats = bookPage.stats(1); // counts the reviews, then reads the stock

            assertThat(stats).isEqualTo(new BookStats(15, 10));
        }

        @Test
        @DisplayName("shows book 1's real review count, 15, after its stock was read")
        void showsTheReviewCountAfterTheStock() {
            inventoryService.copiesInStock(1); // the warehouse screen asked first

            BookStats stats = bookPage.stats(1);

            assertThat(stats).isEqualTo(new BookStats(15, 10));
        }

        @Test
        @DisplayName("keeps the reviews newest first after the book page picked the best ones")
        void keepsTheReviewsNewestFirst() {
            addAFiveStarAndANewerOneStarReviewToBook1();
            List<ReviewResponse> newestFirst = List.copyOf(reviewService.findReviews(1));

            bookPage.topReviews(1);

            assertThat(reviewService.findReviews(1)).containsExactlyElementsOf(newestFirst);
        }

        @Test
        @DisplayName("shows a new review and the new review count right after the review was written")
        void showsANewReviewAtOnce() {
            bookPage.stats(1);
            reviewService.findReviews(1); // the page was visited: both are cached now
            long memberId = newMember("reader@example.com").getId();

            ReviewResponse review = reviewService.writeReview(1, memberId, 4, "Still the Java book I recommend.");

            assertThat(bookPage.stats(1).reviewCount()).isEqualTo(16);
            assertThat(reviewService.findReviews(1)).hasSize(16).first().isEqualTo(review);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("the book page shows the three best reviews, best first")
        void showsTheThreeBestReviews() {
            List<ReviewResponse> threeBest = reviewService.findReviews(1).stream()
                    .sorted(ReviewResponse.BEST_FIRST)
                    .limit(3)
                    .toList();

            assertThat(bookPage.topReviews(1)).containsExactlyElementsOf(threeBest);
        }

        @Test
        @DisplayName("the cached reviews can't be changed by the code that reads them")
        void cachedReviewsAreUnmodifiable() {
            List<ReviewResponse> reviews = reviewService.findReviews(1);

            assertThatThrownBy(reviews::clear).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        @DisplayName("keeps the stock of different books apart")
        void keepsTheStockOfEachBook() {
            assertThat(inventoryService.copiesInStock(1)).isEqualTo(10);

            assertThat(inventoryService.copiesInStock(59)).as("book 59 is out of stock").isZero();
        }

        @Test
        @DisplayName("keeps the review counts of different books apart")
        void keepsTheReviewCountOfEachBook() {
            long reviewsOfBook2 = reviewRepository.countByBookId(2L); // straight from the database

            assertThat(reviewService.countReviews(1)).isEqualTo(15);
            assertThat(reviewService.countReviews(2)).isEqualTo(reviewsOfBook2);
        }
    }

    @Nested
    @DisplayName("Still cached")
    class StillCached {

        @Test
        @DisplayName("reads a book's stats from the database once, then from the cache")
        void cachesTheStats() {
            bookPage.stats(1);
            entityManager.clear(); // or the stock's row would come from the test's persistence context, without SQL
            sqlStatements.reset();

            bookPage.stats(1);

            assertThat(sqlStatements.count()).as("%s", sqlStatements).isZero();
        }

        @Test
        @DisplayName("reads a book's reviews from the database once, then from the cache")
        void cachesTheReviews() {
            reviewService.findReviews(1);
            entityManager.clear();
            sqlStatements.reset();

            reviewService.findReviews(1);
            bookPage.topReviews(1);

            assertThat(sqlStatements.count()).as("%s", sqlStatements).isZero();
        }
    }

    /**
     * Two reviews newer than all seed reviews: a 5-star one, an hour old, and a 1-star one, written now. Then the
     * newest review is the worst one, so "newest first" and "best first" differ for sure.
     */
    private void addAFiveStarAndANewerOneStarReviewToBook1() {
        Book book = bookRepository.getReferenceById(1L);
        clock.setInstant(TestClock.DEFAULT_INSTANT.minus(Duration.ofHours(1)));
        reviewRepository.save(Review.write(book, newMember("fan@example.com"), 5, "I read it twice."));
        clock.reset();
        reviewRepository.save(Review.write(book, newMember("critic@example.com"), 1, "Too long for me."));
    }

    private Member newMember(String email) {
        return memberRepository.save(Member.register(email, "Test Reader", "{noop}secret"));
    }
}
