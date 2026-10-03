package com.example.bookstore.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.book.CategoryRepository;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.SqlStatementCounter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * {@code GET /api/books/{bookId}/reviews} and {@code GET /api/books/{bookId}/rating}. Expected values come from plain
 * SQL over the seed data, or from books and reviews that a test creates (inside the test's transaction).
 */
@IntegrationTest
@DisplayName("S06-02 · Repositories and queries")
class ReviewApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private SqlStatementCounter sqlStatements;

    @Autowired
    private BookRepository books;

    @Autowired
    private CategoryRepository categories;

    @Autowired
    private MemberRepository members;

    @Autowired
    private ReviewRepository reviews;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("returns the first page of a book's reviews: 5, newest first, with each reviewer's name")
        void firstPage() {
            assertThat(mvc.get().uri("/api/books/1/reviews"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.page", page -> assertThat(page).asMap().containsExactlyInAnyOrderEntriesOf(
                            Map.of("size", 5, "number", 0, "totalElements", 15, "totalPages", 3)))
                    .extractingPath("$.content").convertTo(list(ReviewResponse.class))
                    .containsExactlyElementsOf(reviewsOfBook1NewestFirst().subList(0, 5));
        }

        @Test
        @DisplayName("returns another page when asked: ?page=2&size=5 holds the five oldest reviews")
        void anotherPage() {
            assertThat(mvc.get().uri("/api/books/1/reviews?page=2&size=5"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.page.number", number -> assertThat(number).asNumber().isEqualTo(2))
                    .extractingPath("$.content").convertTo(list(ReviewResponse.class))
                    .containsExactlyElementsOf(reviewsOfBook1NewestFirst().subList(10, 15));
        }

        @Test
        @DisplayName("returns a book's number of reviews and its average rating")
        void rating() {
            double average = jdbc.sql("SELECT AVG(rating) FROM reviews WHERE book_id = 1")
                    .query(BigDecimal.class).single().doubleValue();

            assertThat(mvc.get().uri("/api/books/1/rating"))
                    .hasStatusOk()
                    .bodyJson().convertTo(RatingResponse.class)
                    .isEqualTo(new RatingResponse(1L, 15, average));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a page past the last one is empty, but still reports the total")
        void pagePastTheEnd() {
            assertThat(mvc.get().uri("/api/books/1/reviews?page=3&size=5"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content", content -> assertThat(content).asArray().isEmpty())
                    .hasPathSatisfying("$.page.totalElements", total -> assertThat(total).asNumber().isEqualTo(15));
        }

        @Test
        @DisplayName("sorts as the client asks: ?sort=rating,desc&sort=id,asc")
        void clientSort() {
            List<ReviewResponse> expected = reviewsOfBook1("r.rating DESC, r.id ASC");

            assertThat(mvc.get().uri("/api/books/1/reviews?size=15&sort=rating,desc&sort=id,asc"))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.content").convertTo(list(ReviewResponse.class))
                    .containsExactlyElementsOf(expected);
        }

        @Test
        @DisplayName("a book without reviews has an empty page, a review count of 0 and no average (null)")
        void bookWithoutReviews() {
            Book book = newBook();

            assertThat(mvc.get().uri("/api/books/{id}/reviews", book.getId()))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.content", content -> assertThat(content).asArray().isEmpty())
                    .hasPathSatisfying("$.page.totalElements", total -> assertThat(total).asNumber().isEqualTo(0));
            assertThat(mvc.get().uri("/api/books/{id}/rating", book.getId()))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "bookId": %d, "reviewCount": 0, "averageRating": null }""".formatted(book.getId()));
        }

        @Test
        @DisplayName("counts and averages new reviews: ratings 5, 4, 2 and 2 give an average of 3.25")
        void newReviews() {
            Book book = newBook();
            int[] ratings = {5, 4, 2, 2};
            for (int i = 0; i < ratings.length; i++) {
                reviews.save(Review.write(book, members.getReferenceById(i + 1L), ratings[i], "Review " + (i + 1)));
            }

            assertThat(mvc.get().uri("/api/books/{id}/rating", book.getId()))
                    .hasStatusOk()
                    .bodyJson().convertTo(RatingResponse.class)
                    .isEqualTo(new RatingResponse(book.getId(), 4, 3.25));
            assertThat(mvc.get().uri("/api/books/{id}/reviews", book.getId()))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.content[*].reviewer").asArray()
                    .containsExactlyInAnyOrder(memberName(1), memberName(2), memberName(3), memberName(4))
                    .contains("Minji Kim");
        }
    }

    @Nested
    @DisplayName("Queries")
    class Queries {

        @Test
        @DisplayName("reads a page of reviews with at most 3 SQL statements: the book check, the page and the count")
        void pageWithoutNPlusOne() {
            assertThat(mvc.get().uri("/api/books/1/reviews")).hasStatusOk();

            assertThat(sqlStatements.count()).as("%s", sqlStatements).isLessThanOrEqualTo(3);
        }

        @Test
        @DisplayName("lets the database compute the average (avg in SQL) instead of loading the reviews")
        void averageInTheDatabase() {
            assertThat(mvc.get().uri("/api/books/1/rating")).hasStatusOk();

            assertThat(sqlStatements.count()).as("%s", sqlStatements).isLessThanOrEqualTo(3);
            assertThat(sqlStatements.statements()).as("%s", sqlStatements)
                    .anySatisfy(sql -> assertThat(sql).contains("avg("));
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("answers 404 BOOK_NOT_FOUND for the reviews of a book that doesn't exist")
        void reviewsOfAnUnknownBook() {
            assertThat(mvc.get().uri("/api/books/9999/reviews"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "Book 9999 was not found.",
                              "instance": "/api/books/9999/reviews",
                              "code": "BOOK_NOT_FOUND"
                            }""");
        }

        @Test
        @DisplayName("answers 404 BOOK_NOT_FOUND for the rating of a book that doesn't exist")
        void ratingOfAnUnknownBook() {
            assertThat(mvc.get().uri("/api/books/9999/rating"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson()
                    .hasPathSatisfying("$.code", code -> assertThat(code).asString().isEqualTo("BOOK_NOT_FOUND"));
        }
    }

    /** Book 1's 15 reviews in the endpoint's default order, read with plain SQL. */
    private List<ReviewResponse> reviewsOfBook1NewestFirst() {
        return reviewsOfBook1("r.created_at DESC, r.id DESC");
    }

    private List<ReviewResponse> reviewsOfBook1(String orderBy) {
        return jdbc.sql("""
                        SELECT r.id, m.name, r.rating, r.content, r.created_at
                        FROM reviews r JOIN members m ON m.id = r.member_id
                        WHERE r.book_id = 1
                        ORDER BY\s""" + orderBy)
                .query((row, rowNumber) -> new ReviewResponse(row.getLong("id"), row.getString("name"),
                        row.getInt("rating"), row.getString("content"), row.getTimestamp("created_at").toInstant()))
                .list();
    }

    /** A new book in the Programming category, saved in this test's transaction. */
    private Book newBook() {
        return books.save(Book.create("9790000000017", "A Book Nobody Reviewed Yet", "New Author",
                categories.getReferenceById(1L), 20_000, LocalDate.of(2026, 9, 1)));
    }

    private String memberName(long id) {
        return members.findById(id).orElseThrow().getName();
    }
}
