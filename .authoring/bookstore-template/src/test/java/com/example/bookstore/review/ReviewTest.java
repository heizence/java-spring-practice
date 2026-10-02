package com.example.bookstore.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import com.example.bookstore.book.Book;
import com.example.bookstore.book.Category;
import com.example.bookstore.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Template · Review")
class ReviewTest {

    private final Book book = Book.create("9791115881394", "Effective Java", "Joshua Bloch",
            Category.create("Programming"), 36_000, LocalDate.of(2018, 10, 18));
    private final Member member = Member.register("minji@example.com", "Minji Kim", "{noop}secret");

    @ParameterizedTest(name = "rating {0}")
    @ValueSource(ints = {1, 5})
    @DisplayName("accepts ratings from 1 to 5")
    void acceptsRatingsInRange(int rating) {
        assertThat(Review.write(book, member, rating, "Great.").getRating()).isEqualTo(rating);
    }

    @ParameterizedTest(name = "rating {0}")
    @ValueSource(ints = {0, 6})
    @DisplayName("rejects a rating outside 1 to 5")
    void rejectsRatingsOutOfRange(int rating) {
        assertThatThrownBy(() -> Review.write(book, member, rating, "Great."))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("accepts up to 1,000 characters and rejects blank or longer content")
    void contentLength() {
        assertThat(Review.write(book, member, 5, "a".repeat(1_000)).getContent()).hasSize(1_000);
        assertThatThrownBy(() -> Review.write(book, member, 5, "a".repeat(1_001)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Review.write(book, member, 5, " ")).isInstanceOf(IllegalArgumentException.class);
    }
}
