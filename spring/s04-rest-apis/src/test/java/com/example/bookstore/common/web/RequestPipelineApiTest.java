package com.example.bookstore.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Where the request-id filter, the admin token interceptor and the {@code @CurrentMember} argument resolver run:
 *
 * <ul>
 *   <li>the filter for every request, even one that ends in a 404 or that the interceptor rejects;</li>
 *   <li>the interceptor only for {@code /api/admin/**}, before the controller;</li>
 *   <li>the resolver only for controller parameters marked {@code @CurrentMember}.</li>
 * </ul>
 */
@IntegrationTest
@DisplayName("S04-05 · Filters, interceptors and argument resolvers")
class RequestPipelineApiTest {

    private static final String REQUEST_ID = "X-Request-Id";
    private static final String ADMIN_TOKEN = "X-Admin-Token";
    private static final String MEMBER_ID = "X-Member-Id";

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private AdminProperties adminProperties;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("filter: a request without an id gets a new UUID in the X-Request-Id response header")
        void newRequestId() {
            MvcTestResult result = mvc.get().uri("/api/books/1").exchange();

            assertThat(result).hasStatusOk().containsHeader(REQUEST_ID);
            assertThat(UUID.fromString(requestIdOf(result)).toString()).isEqualTo(requestIdOf(result));
        }

        @Test
        @DisplayName("filter: a request that sends an id gets the same id back")
        void keepsTheRequestId() {
            assertThat(mvc.get().uri("/api/books/1").header(REQUEST_ID, "checkout-42.retry_1"))
                    .hasHeader(REQUEST_ID, "checkout-42.retry_1");
        }

        @Test
        @DisplayName("interceptor: the admin API answers a request with the admin token")
        void adminWithToken() {
            assertThat(mvc.get().uri("/api/admin/books/stats").header(ADMIN_TOKEN, adminProperties.token()))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "books": 60, "categories": 6, "outOfStock": 2 }""");
        }

        @Test
        @DisplayName("interceptor: the admin API answers 401 to a request without the admin token")
        void adminWithoutToken() {
            assertThat(mvc.get().uri("/api/admin/books/stats"))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Unauthorized",
                              "status": 401,
                              "detail": "Send the admin token in the X-Admin-Token header.",
                              "instance": "/api/admin/books/stats",
                              "code": "UNAUTHORIZED"
                            }""");
        }

        @Test
        @DisplayName("interceptor: the rest of the API needs no admin token")
        void publicApiWithoutToken() {
            assertThat(mvc.get().uri("/api/books/1")).hasStatusOk();
            assertThat(mvc.get().uri("/api/books/1/availability")).hasStatusOk();
        }

        @Test
        @DisplayName("resolver: GET /api/members/me answers the member named by X-Member-Id")
        void currentMember() {
            assertThat(mvc.get().uri("/api/members/me").header(MEMBER_ID, "1"))
                    .hasStatusOk()
                    .bodyJson().isStrictlyEqualTo("""
                            { "id": 1, "email": "minji@example.com", "name": "Minji Kim", "role": "USER" }""");
        }

        @Test
        @DisplayName("resolver: member 20 is the administrator")
        void administrator() {
            assertThat(mvc.get().uri("/api/members/me").header(MEMBER_ID, "20"))
                    .hasStatusOk()
                    .bodyJson()
                    .hasPathSatisfying("$.email", email -> assertThat(email).isEqualTo("admin@example.com"))
                    .hasPathSatisfying("$.role", role -> assertThat(role).isEqualTo("ADMIN"));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("filter: two requests without an id get two different ids")
        void differentIds() {
            String first = requestIdOf(mvc.get().uri("/api/books/1").exchange());
            String second = requestIdOf(mvc.get().uri("/api/books/1").exchange());

            assertThat(first).isNotEqualTo(second);
        }

        @Test
        @DisplayName("filter: an id of 64 characters is kept, one of 65 is replaced")
        void idLength() {
            String longest = "a".repeat(64);
            String tooLong = "a".repeat(65);

            assertThat(mvc.get().uri("/api/books/1").header(REQUEST_ID, longest)).hasHeader(REQUEST_ID, longest);
            assertThat(requestIdOf(mvc.get().uri("/api/books/1").header(REQUEST_ID, tooLong).exchange()))
                    .isNotEqualTo(tooLong)
                    .hasSize(36);
        }

        @Test
        @DisplayName("filter: runs even for a path that no controller handles")
        void filterRunsForUnknownPaths() {
            assertThat(mvc.get().uri("/api/nothing-here"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .containsHeader(REQUEST_ID);
        }

        @Test
        @DisplayName("filter: runs even for a request that the interceptor rejects")
        void filterRunsBeforeTheInterceptor() {
            assertThat(mvc.get().uri("/api/admin/books/stats").header(REQUEST_ID, "admin-call-7"))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .hasHeader(REQUEST_ID, "admin-call-7");
        }

        @Test
        @DisplayName("resolver: other endpoints need no X-Member-Id")
        void otherEndpointsNeedNoMember() {
            assertThat(mvc.get().uri("/api/books")).hasStatusOk();
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "X-Request-Id: {0}")
        @ValueSource(strings = {"two words", "<script>", "line\r\nbreak", "한글"})
        @DisplayName("filter: an id with other characters is replaced by a new UUID, never echoed")
        void invalidRequestId(String requestId) {
            String answered = requestIdOf(mvc.get().uri("/api/books/1").header(REQUEST_ID, requestId).exchange());

            assertThat(answered).isNotEqualTo(requestId);
            assertThat(UUID.fromString(answered).toString()).isEqualTo(answered);
        }

        @Test
        @DisplayName("interceptor: a token that isn't exactly the admin token gets 401: other case, prefix, longer")
        void wrongToken() {
            String token = adminProperties.token();
            List<String> wrongTokens = List.of("wrong-token", token.toUpperCase(Locale.ROOT),
                    token.substring(0, token.length() - 1), token + "2", " " + token, "");

            for (String wrongToken : wrongTokens) {
                assertThat(mvc.get().uri("/api/admin/books/stats").header(ADMIN_TOKEN, wrongToken))
                        .as("X-Admin-Token: \"%s\"", wrongToken)
                        .hasStatus(HttpStatus.UNAUTHORIZED)
                        .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
            }
        }

        @Test
        @DisplayName("interceptor: guards every path under /api/admin, even one that doesn't exist")
        void unknownAdminPath() {
            assertThat(mvc.get().uri("/api/admin/secret-report")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        @DisplayName("resolver: a request without X-Member-Id gets 401")
        void noMember() {
            assertThat(mvc.get().uri("/api/members/me"))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Unauthorized",
                              "status": 401,
                              "detail": "Send the id of an existing member in the X-Member-Id header.",
                              "instance": "/api/members/me",
                              "code": "UNAUTHORIZED"
                            }""");
        }

        @ParameterizedTest(name = "X-Member-Id: {0}")
        @ValueSource(strings = {"999", "0", "-1", "abc", "1.0", " 1", "", "12345678901234567890"})
        @DisplayName("resolver: an id that names no member gets 401")
        void unknownMember(String memberId) {
            assertThat(mvc.get().uri("/api/members/me").header(MEMBER_ID, memberId))
                    .hasStatus(HttpStatus.UNAUTHORIZED)
                    .bodyJson().extractingPath("$.code").isEqualTo("UNAUTHORIZED");
        }
    }

    private static String requestIdOf(MvcTestResult result) {
        String requestId = result.getResponse().getHeader(REQUEST_ID);
        assertThat(requestId).as("X-Request-Id response header").isNotNull();
        return requestId;
    }
}
