package com.example.bookstore.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.BookRepository;
import com.example.bookstore.member.Member;
import com.example.bookstore.member.MemberRepository;
import com.example.bookstore.review.Review;
import com.example.bookstore.review.ReviewRepository;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
@DisplayName("Template · JPA auditing")
class JpaAuditingTest {

    @Autowired
    private TestClock clock;

    @Autowired
    private Clock applicationClock;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private MemberRepository members;

    @Autowired
    private BookRepository books;

    @Autowired
    private ReviewRepository reviews;

    @Test
    @DisplayName("the application's Clock is the test clock in integration tests")
    void injectsTheTestClock() {
        assertThat(applicationClock).isSameAs(clock);
        assertThat(applicationClock.instant()).isEqualTo(TestClock.DEFAULT_INSTANT);
    }

    @Test
    @DisplayName("a new entity gets createdAt and updatedAt from the clock")
    void fillsBothTimesOnInsert() {
        Member member = members.saveAndFlush(Member.register("audit@example.com", "Audit", "{noop}secret"));

        assertThat(member.getCreatedAt()).isEqualTo(TestClock.DEFAULT_INSTANT);
        assertThat(member.getUpdatedAt()).isEqualTo(TestClock.DEFAULT_INSTANT);
    }

    @Test
    @DisplayName("a changed entity gets a new updatedAt and keeps its createdAt")
    void updatesOnlyUpdatedAt() {
        Book book = books.findById(1L).orElseThrow();
        Instant later = TestClock.DEFAULT_INSTANT.plus(Duration.ofHours(2));
        clock.setInstant(later);

        book.changePrice(38_000);
        entityManager.flush();

        assertThat(book.getCreatedAt()).isEqualTo(Instant.parse("2026-07-01T00:00:00Z"));
        assertThat(book.getUpdatedAt()).isEqualTo(later);
    }

    @Test
    @DisplayName("an entity without updated_at gets its createdAt too")
    void fillsCreatedAtOfOtherEntities() {
        clock.advance(Duration.ofMinutes(5));

        Review review = reviews.saveAndFlush(Review.write(books.getReferenceById(2L), members.getReferenceById(1L),
                5, "Short and to the point."));

        assertThat(review.getCreatedAt()).isEqualTo(TestClock.DEFAULT_INSTANT.plus(Duration.ofMinutes(5)));
    }
}
