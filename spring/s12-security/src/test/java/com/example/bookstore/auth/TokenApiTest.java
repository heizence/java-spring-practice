package com.example.bookstore.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.time.Duration;
import java.util.Date;

import com.example.bookstore.common.security.JwtProperties;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import com.example.bookstore.support.TestTokens;
import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * {@code POST /api/auth/token}. The tests read the issued tokens with {@link TestTokens#verify}, which checks the
 * signature with the configured secret but not the expiry: the token's times come from the {@link TestClock}.
 */
@IntegrationTest
@DisplayName("S12-03 · Issuing JWTs")
class TokenApiTest {

    private static final String SEED_PASSWORD = "bookstore123";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private TestClock clock;

    private TestTokens tokens;

    @BeforeEach
    void createTokens() {
        tokens = new TestTokens(jwtProperties, clock);
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("answers the right email and password with a Bearer token that lives 15 minutes")
        void issuesABearerToken() {
            assertThat(requestToken("minji@example.com", SEED_PASSWORD))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.accessToken", token -> assertThat(token).asString().isNotBlank())
                    .hasPathSatisfying("$.tokenType", type -> assertThat(type).asString().isEqualTo("Bearer"))
                    .hasPathSatisfying("$.expiresIn", seconds -> assertThat(seconds).asNumber().isEqualTo(900));
        }

        @Test
        @DisplayName("signs the token with the configured secret, using HS256")
        void signsWithTheSecret() {
            SignedJWT token = tokens.verify(issuedToken("minji@example.com", SEED_PASSWORD));

            assertThat(token.getHeader().getAlgorithm()).isEqualTo(JWSAlgorithm.HS256);
        }

        @Test
        @DisplayName("names the member and their role: sub is the member's id, role is USER")
        void namesTheMemberAndRole() throws ParseException {
            JWTClaimsSet claims = tokens.verify(issuedToken("minji@example.com", SEED_PASSWORD)).getJWTClaimsSet();

            assertThat(claims.getIssuer()).isEqualTo("bookstore");
            assertThat(claims.getSubject()).isEqualTo("1");
            assertThat(claims.getStringClaim("role")).isEqualTo("USER");
        }

        @Test
        @DisplayName("gives the administrator the role ADMIN")
        void adminRole() throws ParseException {
            JWTClaimsSet claims = tokens.verify(issuedToken("admin@example.com", SEED_PASSWORD)).getJWTClaimsSet();

            assertThat(claims.getSubject()).isEqualTo("20");
            assertThat(claims.getStringClaim("role")).isEqualTo("ADMIN");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("is issued now by the application's clock and expires 15 minutes later")
        void timesComeFromTheClock() throws ParseException {
            clock.advance(Duration.ofHours(3));

            JWTClaimsSet claims = tokens.verify(issuedToken("minji@example.com", SEED_PASSWORD)).getJWTClaimsSet();

            assertThat(claims.getIssueTime()).isEqualTo(Date.from(clock.instant()));
            assertThat(claims.getExpirationTime()).isEqualTo(Date.from(clock.instant().plus(Duration.ofMinutes(15))));
        }

        @Test
        @DisplayName("contains exactly iss, sub, role, iat and exp: no email, no name")
        void containsNoPersonalData() throws ParseException {
            JWTClaimsSet claims = tokens.verify(issuedToken("minji@example.com", SEED_PASSWORD)).getJWTClaimsSet();

            assertThat(claims.getClaims()).containsOnlyKeys("iss", "sub", "role", "iat", "exp");
        }

        @Test
        @DisplayName("finds the member whatever the letter case of the email")
        void emailIsCaseInsensitive() {
            assertThat(requestToken("Minji@Example.COM", SEED_PASSWORD)).hasStatusOk();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("a wrong password is 401 INVALID_CREDENTIALS")
        void wrongPassword() {
            assertThat(requestToken("minji@example.com", "bookstore124"))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Unauthorized",
                              "status": 401,
                              "detail": "The email or password is wrong.",
                              "instance": "/api/auth/token",
                              "code": "INVALID_CREDENTIALS"
                            }""");
        }

        @Test
        @DisplayName("an unknown email gets exactly the same answer as a wrong password")
        void unknownEmailLooksLikeAWrongPassword() {
            MvcTestResult wrongPassword = requestToken("minji@example.com", "bookstore124");
            MvcTestResult unknownEmail = requestToken("nobody@example.com", SEED_PASSWORD);

            assertThat(unknownEmail.getResponse().getStatus()).isEqualTo(wrongPassword.getResponse().getStatus());
            assertThat(unknownEmail).bodyJson().isStrictlyEqualTo(body(wrongPassword));
        }

        @Test
        @DisplayName("a password in the wrong letter case is wrong")
        void passwordIsCaseSensitive() {
            assertThat(requestToken("minji@example.com", "BOOKSTORE123")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("a missing email or password is 400")
        void missingFields() {
            assertThat(mvc.post().uri("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"email\": \"minji@example.com\" }"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }
    }

    private MvcTestResult requestToken(String email, String password) {
        return mvc.post().uri("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                .content("""
                        { "email": "%s", "password": "%s" }""".formatted(email, password))
                .exchange();
    }

    private String issuedToken(String email, String password) {
        MvcTestResult result = requestToken(email, password);
        assertThat(result).as("POST /api/auth/token for %s", email).hasStatusOk();
        return JsonPath.read(body(result), "$.accessToken");
    }

    private static String body(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
