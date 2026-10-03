package com.example.bookstore.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import com.example.bookstore.member.Role;
import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestClock;
import com.example.bookstore.support.TestTokens;
import com.nimbusds.jwt.JWTClaimsSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Real tokens through the resource server: {@code Authorization: Bearer …}. The tokens come from {@link TestTokens},
 * which signs them like the bookstore does, so these tests don't depend on S12-03. Their times come from the
 * {@link TestClock}, like the bookstore's own tokens.
 */
@IntegrationTest
@DisplayName("S12-04 · Authenticating requests with JWTs")
class BearerTokenTest {

    /** A key of the right length that isn't the bookstore's: whoever has it can sign tokens, but not valid ones. */
    private static final byte[] ANOTHER_SECRET =
            "an attacker's key, not the bookstore's own key".getBytes(StandardCharsets.UTF_8);

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
        @DisplayName("GET /api/members/me returns the member that the token names")
        void meReturnsTheTokensMember() {
            assertThat(getMe(tokens.validFor(1, Role.USER)))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "id": 1, "email": "minji@example.com", "name": "Minji Kim", "role": "USER" }""");
        }

        @Test
        @DisplayName("GET /api/members/me with the administrator's token returns the administrator")
        void meReturnsTheAdmin() {
            assertThat(getMe(tokens.validFor(20, Role.ADMIN)))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "id": 20, "email": "admin@example.com", "name": "Admin", "role": "ADMIN" }""");
        }

        @Test
        @DisplayName("the role claim becomes the request's role: ADMIN opens admin paths, USER doesn't")
        void roleClaimBecomesTheRole() {
            assertThat(get("/api/admin/members/1", tokens.validFor(20, Role.ADMIN))).hasStatusOk();
            assertThat(get("/api/admin/members/1", tokens.validFor(1, Role.USER))).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("accepts a token until it expires by the application's clock")
        void acceptsATokenBeforeItExpires() {
            String token = tokens.validFor(1, Role.USER);

            clock.advance(Duration.ofMinutes(14));

            assertThat(getMe(token)).hasStatusOk();
        }

        @Test
        @DisplayName("rejects a token once the application's clock has passed its expiry")
        void rejectsATokenAfterItExpires() {
            String token = tokens.validFor(1, Role.USER);

            clock.advance(Duration.ofMinutes(17)); // past the 15 minutes, and past the 60 s of leeway

            assertThat(getMe(token)).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("rejects a token without an expiry: it would be valid forever")
        void rejectsATokenWithoutExpiry() {
            String token = tokens.sign(tokens.claimsFor(1, Role.USER).expirationTime(null).build());

            assertThat(getMe(token)).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("rejects a token from another issuer, even when it's signed with the right key")
        void rejectsAnotherIssuer() {
            String token = tokens.sign(tokens.claimsFor(1, Role.USER).issuer("someone-else").build());

            assertThat(getMe(token)).hasStatus(HttpStatus.UNAUTHORIZED);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("an expired token is 401 with the UNAUTHORIZED problem detail")
        void expiredTokenIsAProblemDetail() {
            String expired = tokens.sign(tokens.claimsFor(1, Role.USER)
                    .issueTime(Date.from(clock.instant().minus(Duration.ofHours(2))))
                    .expirationTime(Date.from(clock.instant().minus(Duration.ofHours(1))))
                    .build());

            assertThat(getMe(expired))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Unauthorized",
                              "status": 401,
                              "detail": "A valid access token is required.",
                              "instance": "/api/members/me",
                              "code": "UNAUTHORIZED"
                            }""");
        }

        @Test
        @DisplayName("a token signed with another key is 401 with the UNAUTHORIZED problem detail")
        void forgedSignature() {
            String forged = TestTokens.sign(tokens.claimsFor(20, Role.ADMIN).build(), ANOTHER_SECRET);

            assertThat(getMe(forged))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("an unsigned token (alg: none) is 401")
        void unsignedToken() {
            String unsigned = TestTokens.unsigned(tokens.claimsFor(20, Role.ADMIN).build());

            assertThat(getMe(unsigned))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("something that isn't a JWT at all is 401")
        void malformedToken() {
            assertThat(getMe("not-a-jwt"))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
        }

        @Test
        @DisplayName("a token whose member doesn't exist is 404 MEMBER_NOT_FOUND")
        void unknownMember() {
            JWTClaimsSet claims = tokens.claimsFor(9999, Role.USER).build();

            assertThat(getMe(tokens.sign(claims)))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().extractingPath("$.code").isEqualTo("MEMBER_NOT_FOUND");
        }
    }

    private MvcTestResult getMe(String token) {
        return get("/api/members/me", token);
    }

    private MvcTestResult get(String uri, String token) {
        return mvc.get().uri(uri).header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange();
    }
}
