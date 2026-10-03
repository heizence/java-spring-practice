package com.example.bookstore.book;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestUsers;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** {@code PUT /api/books/{id}/price}: only administrators change prices. Book 1 costs 36,000 won. */
@IntegrationTest
@DisplayName("S12-05 · Authorization rules")
class BookPriceAuthorizationTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private EntityManager entityManager;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("an administrator changes a book's price")
        void adminChangesThePrice() {
            assertThat(changePrice(1, 39_000, TestUsers.admin()))
                    .hasStatusOk()
                    .bodyJson().extractingPath("$.price").isEqualTo(39_000);
            assertThat(priceOfBook1()).isEqualTo(39_000);
        }

        @Test
        @DisplayName("a member who isn't an administrator gets 403 FORBIDDEN")
        void memberIsForbidden() {
            assertThat(changePrice(1, 1_000, TestUsers.minji()))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson().extractingPath("$.code").isEqualTo("FORBIDDEN");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("a refused price change leaves the price as it was")
        void refusedChangeChangesNothing() {
            changePrice(1, 1_000, TestUsers.minji());

            assertThat(priceOfBook1()).isEqualTo(36_000);
        }

        @Test
        @DisplayName("reading books stays open to everyone")
        void readingStaysPublic() {
            assertThat(mvc.get().uri("/api/books/1")).hasStatusOk();
            assertThat(mvc.get().uri("/api/books/1").with(TestUsers.minji())).hasStatusOk();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an administrator's change of a book that doesn't exist is 404")
        void unknownBook() {
            assertThat(changePrice(9999, 39_000, TestUsers.admin()))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("BOOK_NOT_FOUND");
        }
    }

    private MvcTestResult changePrice(long bookId, long price, JwtRequestPostProcessor user) {
        return mvc.put().uri("/api/books/{id}/price", bookId).with(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"price\": %d }".formatted(price))
                .exchange();
    }

    /** Writes pending changes first: SQL through JdbcClient doesn't make Hibernate flush them. */
    private long priceOfBook1() {
        entityManager.flush();
        return jdbc.sql("SELECT price FROM books WHERE id = 1").query(Long.class).single();
    }
}
