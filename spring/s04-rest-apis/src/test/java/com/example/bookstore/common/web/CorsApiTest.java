package com.example.bookstore.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * CORS for the shop's front end at {@code https://shop.example.com}. A browser sends an {@code Origin} header with
 * every request from another site, and asks first with a <em>preflight</em> request ({@code OPTIONS} with
 * {@code Access-Control-Request-Method}) before it sends anything but a simple {@code GET}.
 */
@IntegrationTest
@DisplayName("S04-10 · File upload and CORS")
class CorsApiTest {

    private static final String SHOP = "https://shop.example.com";

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("CORS: a preflight from the shop for a PATCH with If-Match is allowed")
        void preflightFromTheShop() {
            MvcTestResult result = preflight(SHOP, "PATCH", "If-Match, Content-Type");

            assertThat(result)
                    .hasStatusOk()
                    .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SHOP)
                    .hasHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
            assertThat(headerOf(result, HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)).contains("PATCH");
            assertThat(headerOf(result, HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)).containsIgnoringCase("If-Match")
                    .containsIgnoringCase("Content-Type");
        }

        @Test
        @DisplayName("CORS: a GET from the shop is allowed, and it may read ETag, Location and X-Request-Id")
        void getFromTheShop() {
            MvcTestResult result = mvc.get().uri("/api/books/1/availability").header(HttpHeaders.ORIGIN, SHOP)
                    .exchange();

            assertThat(result)
                    .hasStatusOk()
                    .hasHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, SHOP);
            assertThat(headerOf(result, HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)).containsIgnoringCase("ETag")
                    .containsIgnoringCase("Location").containsIgnoringCase("X-Request-Id");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("CORS: a request without Origin, from a server or the same site, gets no CORS headers")
        void noOrigin() {
            assertThat(mvc.get().uri("/api/books/1/availability"))
                    .hasStatusOk()
                    .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        }

        @Test
        @DisplayName("CORS: the shop may not send cookies or other credentials")
        void noCredentials() {
            assertThat(preflight(SHOP, "PUT", "Content-Type"))
                    .hasStatusOk()
                    .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
        }
    }

    @Nested
    @DisplayName("Errors")
    class Errors {

        @ParameterizedTest(name = "Origin: {0}")
        @ValueSource(strings = {"https://evil.example.com", "http://shop.example.com", "https://shop.example.com:8443",
                "https://shop.example.com.evil.com"})
        @DisplayName("CORS: a preflight from any other origin is refused with 403")
        void otherOrigin(String origin) {
            assertThat(preflight(origin, "PATCH", "If-Match"))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .doesNotContainHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
        }

        @Test
        @DisplayName("CORS: a GET from another origin is refused with 403")
        void getFromAnotherOrigin() {
            assertThat(mvc.get().uri("/api/books/1/availability")
                    .header(HttpHeaders.ORIGIN, "https://evil.example.com"))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("CORS: the shop may not send the admin token")
        void noAdminToken() {
            assertThat(preflight(SHOP, "GET", "X-Admin-Token"))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("CORS: methods other than GET, POST, PUT, PATCH and DELETE are refused")
        void otherMethod() {
            assertThat(preflight(SHOP, "TRACE", "Content-Type"))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    private static String headerOf(MvcTestResult result, String name) {
        String value = result.getResponse().getHeader(name);
        assertThat(value).as("response header %s", name).isNotNull();
        return value;
    }

    private MvcTestResult preflight(String origin, String method, String requestHeaders) {
        return mvc.options().uri("/api/books/1")
                .header(HttpHeaders.ORIGIN, origin)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, method)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, requestHeaders)
                .exchange();
    }
}
