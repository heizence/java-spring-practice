package com.example.bookstore.review;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.data.web.SortDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The reviews on the book page: {@code GET /api/books/{bookId}/reviews} and {@code GET /api/books/{bookId}/rating}. */
@RestController
@RequestMapping("/api/books/{bookId}")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * A page of a book's reviews, newest first by default: {@code GET /api/books/1/reviews?page=0&size=5}. The id
     * breaks ties between reviews written at the same moment, so that every review appears on exactly one page.
     */
    @GetMapping("/reviews")
    public PagedModel<ReviewResponse> getReviews(
            @PathVariable long bookId,
            @PageableDefault(size = 5)
            @SortDefault.SortDefaults({
                    @SortDefault(sort = "createdAt", direction = Sort.Direction.DESC),
                    @SortDefault(sort = "id", direction = Sort.Direction.DESC)}) Pageable pageable) {
        return new PagedModel<>(reviewService.findReviews(bookId, pageable));
    }

    /** How many reviews a book has and its average rating: {@code GET /api/books/1/rating}. */
    @GetMapping("/rating")
    public RatingResponse getRating(@PathVariable long bookId) {
        return reviewService.getRating(bookId);
    }
}
