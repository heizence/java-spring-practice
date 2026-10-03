package com.example.bookstore.common.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.support.IntegrationTest;
import com.example.bookstore.support.TestUsers;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Which requests the filter chain lets through, and how it answers the others. Requests "with a token" use
 * {@link TestUsers}, which signs them in without a real token, so these tests don't depend on S12-03 or S12-04.
 */
@IntegrationTest
@DisplayName("S12-01 · The security filter chain")
class SecurityFilterChainTest {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("anyone can read the catalog, without a token")
        void catalogIsPublic() {
            assertThat(mvc.get().uri("/api/books")).hasStatusOk();
            assertThat(mvc.get().uri("/api/books/1")).hasStatusOk();
        }

        @Test
        @DisplayName("anyone can call sign-up and sign-in: they answer an empty body with 400, not 401")
        void signUpAndSignInArePublic() {
            assertThat(mvc.post().uri("/api/members").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
            assertThat(mvc.post().uri("/api/auth/token").contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("a request without a token to anything else gets 401")
        void otherEndpointsNeedAToken() {
            assertThat(mvc.get().uri("/api/wallet")).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(mvc.get().uri("/api/orders/1")).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(mvc.get().uri("/api/members/me")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("a signed-in member gets through to a protected endpoint")
        void signedInMemberGetsThrough() {
            assertThat(mvc.get().uri("/api/wallet").with(TestUsers.minji()))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "memberId": 1, "balance": 100000 }""");
        }

        @Test
        @DisplayName("an admin path refuses a member who isn't an administrator: 403")
        void adminPathRefusesMembers() {
            assertThat(mvc.get().uri("/api/admin/members/1").with(TestUsers.minji()))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("an administrator can use admin paths")
        void adminPathAcceptsAdministrators() {
            assertThat(mvc.get().uri("/api/admin/members/1").with(TestUsers.admin()))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "id": 1, "email": "minji@example.com", "name": "Minji Kim", "role": "USER" }""");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("only reading books is public: changing a price without a token gets 401")
        void onlyGetIsPublicForBooks() {
            assertThat(mvc.put().uri("/api/books/1/price").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"price\": 1000 }"))
                    .hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("only POST is public for sign-up and sign-in: another method without a token gets 401")
        void onlyPostIsPublicForTheAuthenticationEndpoints() {
            assertThat(mvc.get().uri("/api/members")).hasStatus(HttpStatus.UNAUTHORIZED);
            assertThat(mvc.delete().uri("/api/auth/token")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("denies by default: a path that no rule names and no controller handles gets 401, not 404")
        void deniesByDefault() {
            assertThat(mvc.get().uri("/api/nothing-here")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("an admin path without a token gets 401, not 403: authentication comes first")
        void adminPathWithoutTokenIs401() {
            assertThat(mvc.get().uri("/api/admin/members/1")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("POST requests without a CSRF token aren't refused: the API uses no cookies")
        void noCsrfTokenNeeded() {
            assertThat(mvc.post().uri("/api/auth/token").contentType(MediaType.APPLICATION_JSON)
                    .content("{ \"email\": \"\", \"password\": \"\" }"))
                    .hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("creates no HTTP session, also when it refuses a request")
        void createsNoSession() {
            MvcTestResult refused = mvc.get().uri("/api/wallet").exchange();
            MvcTestResult accepted = mvc.get().uri("/api/wallet").with(TestUsers.minji()).exchange();

            assertThat(refused.getResponse().getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(refused.getRequest().getSession(false)).as("session after a refused request").isNull();
            assertThat(accepted.getRequest().getSession(false)).as("session after an accepted request").isNull();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @Test
        @DisplayName("401 is a problem detail with code UNAUTHORIZED")
        void unauthorizedIsAProblemDetail() {
            assertThat(mvc.get().uri("/api/wallet"))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Unauthorized",
                              "status": 401,
                              "detail": "A valid access token is required.",
                              "instance": "/api/wallet",
                              "code": "UNAUTHORIZED"
                            }""");
        }

        @Test
        @DisplayName("401 names the scheme the client should use: WWW-Authenticate: Bearer")
        void unauthorizedNamesTheBearerScheme() {
            assertThat(mvc.get().uri("/api/wallet")).hasHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        }

        @Test
        @DisplayName("403 from a URL rule is a problem detail with code FORBIDDEN")
        void forbiddenIsAProblemDetail() {
            assertThat(mvc.get().uri("/api/admin/members/1").with(TestUsers.minji()))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Forbidden",
                              "status": 403,
                              "detail": "You are not allowed to do this.",
                              "instance": "/api/admin/members/1",
                              "code": "FORBIDDEN"
                            }""");
        }

        @Test
        @DisplayName("403 from @PreAuthorize in a service is the same problem detail, not a 500")
        void methodSecurityDenialIsAProblemDetail() {
            assertThat(mvc.get().uri("/api/wallets/2").with(TestUsers.minji()))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Forbidden",
                              "status": 403,
                              "detail": "You are not allowed to do this.",
                              "instance": "/api/wallets/2",
                              "code": "FORBIDDEN"
                            }""");
        }
    }
}
