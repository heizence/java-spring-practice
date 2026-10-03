package com.example.bookstore.member;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** {@code POST /api/members}, checked through the API and, for what the API never shows, in the database. */
@IntegrationTest
@DisplayName("S12-02 · Password hashing and sign-up")
class SignUpApiTest {

    private static final String PASSWORD = "correct-horse-7";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JdbcClient jdbc;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("signs up a customer: 201 with the new member and its address, without a password")
        void signsUpACustomer() {
            MvcTestResult result = signUp("yuri@example.com", "Yuri Han", PASSWORD);

            long id = newMemberId("yuri@example.com");
            assertThat(result)
                    .hasStatus(HttpStatus.CREATED)
                    .hasHeader(HttpHeaders.LOCATION, "/api/admin/members/" + id)
                    .bodyJson().isStrictlyEqualTo("""
                            { "id": %d, "email": "yuri@example.com", "name": "Yuri Han", "role": "USER" }"""
                            .formatted(id));
        }

        @Test
        @DisplayName("stores the password only as a hash from the delegating encoder: {bcrypt}$2…")
        void storesADelegatingEncoderHash() {
            signUp("yuri@example.com", "Yuri Han", PASSWORD);

            String hash = storedHash("yuri@example.com");
            assertThat(hash).startsWith("{bcrypt}$2").doesNotContain(PASSWORD);
            assertThat(passwordEncoder.matches(PASSWORD, hash)).as("the hash matches the password").isTrue();
        }

        @Test
        @DisplayName("stores the email in lower case, and the member as a customer (USER)")
        void storesTheMember() {
            signUp("Yuri@Example.COM", "Yuri Han", PASSWORD);

            assertThat(jdbc.sql("SELECT email, role FROM members WHERE email = 'yuri@example.com'")
                    .query().singleRow())
                    .containsExactlyInAnyOrderEntriesOf(Map.of("email", "yuri@example.com", "role", "USER"));
        }

        @Test
        @DisplayName("opens an empty wallet for the new member: every member has exactly one")
        void opensAWallet() {
            signUp("yuri@example.com", "Yuri Han", PASSWORD);

            assertThat(jdbc.sql("SELECT balance FROM wallets WHERE member_id = :id")
                    .param("id", newMemberId("yuri@example.com"))
                    .query(Long.class).list())
                    .containsExactly(0L);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("salts every hash: two members with the same password get different hashes")
        void saltsEveryHash() {
            signUp("yuri@example.com", "Yuri Han", PASSWORD);
            signUp("sora@example.com", "Sora Park", PASSWORD);

            assertThat(storedHash("yuri@example.com")).isNotEqualTo(storedHash("sora@example.com"));
        }

        @Test
        @DisplayName("ignores a role in the request: everyone who signs up is a customer")
        void ignoresARoleInTheRequest() {
            String body = """
                    { "email": "yuri@example.com", "name": "Yuri Han", "password": "%s", "role": "ADMIN" }"""
                    .formatted(PASSWORD);

            assertThat(mvc.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).content(body))
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson().extractingPath("$.role").isEqualTo("USER");
        }

        @Test
        @DisplayName("accepts a password of exactly 72 bytes: 24 Korean letters")
        void acceptsSeventyTwoBytes() {
            assertThat(signUp("yuri@example.com", "Yuri Han", "한".repeat(24))).hasStatus(HttpStatus.CREATED);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an email that is already registered, in any letter case, is 409 DUPLICATE_EMAIL")
        void duplicateEmail() {
            assertThat(signUp("Minji@Example.COM", "Another Minji", PASSWORD))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Conflict",
                              "status": 409,
                              "detail": "The email address minji@example.com is already registered.",
                              "instance": "/api/members",
                              "code": "DUPLICATE_EMAIL"
                            }""");
        }

        @Test
        @DisplayName("a duplicate email changes nothing: still 20 members and 20 wallets")
        void duplicateEmailSavesNothing() {
            signUp("minji@example.com", "Another Minji", PASSWORD);

            assertThat(jdbc.sql("SELECT COUNT(*) FROM members").query(Long.class).single()).isEqualTo(20);
            assertThat(jdbc.sql("SELECT COUNT(*) FROM wallets").query(Long.class).single()).isEqualTo(20);
        }

        @Test
        @DisplayName("a password longer than 72 bytes is 400, also when it has fewer than 72 characters")
        void passwordLongerThanBcryptCanUse() {
            String twentyFiveKoreanLetters = "한".repeat(25); // 25 characters, 75 bytes in UTF-8

            assertThat(signUp("yuri@example.com", "Yuri Han", twentyFiveKoreanLetters))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.code").isEqualTo("INVALID_INPUT");
            assertThat(jdbc.sql("SELECT COUNT(*) FROM members").query(Long.class).single()).isEqualTo(20);
        }

        @Test
        @DisplayName("an invalid email, a blank name and a short password are 400, with every invalid field")
        void invalidRequest() {
            assertThat(signUp("not-an-email", " ", "short"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().extractingPath("$.errors[*].field")
                    .asArray().containsExactly("email", "name", "password");
        }
    }

    private MvcTestResult signUp(String email, String name, String password) {
        String body = """
                { "email": "%s", "name": "%s", "password": "%s" }""".formatted(email, name, password);
        return mvc.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).content(body).exchange();
    }

    private long newMemberId(String email) {
        return jdbc.sql("SELECT id FROM members WHERE email = :email").param("email", email)
                .query(Long.class).single();
    }

    private String storedHash(String email) {
        return jdbc.sql("SELECT password_hash FROM members WHERE email = :email").param("email", email)
                .query(String.class).single();
    }
}
