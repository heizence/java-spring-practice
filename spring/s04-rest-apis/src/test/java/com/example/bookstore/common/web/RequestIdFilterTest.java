package com.example.bookstore.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** {@link RequestIdFilter} on its own: a plain object, called with mock requests, without Spring. */
@DisplayName("S04-05 · Filters, interceptors and argument resolvers")
class RequestIdFilterTest {

    private final RequestIdFilter filter = new RequestIdFilter();

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("filter: passes every request on to the rest of the chain, exactly once")
        void callsTheChainOnce() throws Exception {
            var calls = new AtomicInteger();
            FilterChain chain = (request, response) -> calls.incrementAndGet();

            filter.doFilter(new MockHttpServletRequest("GET", "/api/books"), new MockHttpServletResponse(), chain);

            assertThat(calls).hasValue(1);
        }

        @Test
        @DisplayName("filter: sets the response header before the rest of the chain runs")
        void setsTheHeaderFirst() throws Exception {
            var headerDuringChain = new AtomicReference<String>();
            var response = new MockHttpServletResponse();
            FilterChain chain = (request, chainResponse) ->
                    headerDuringChain.set(((MockHttpServletResponse) chainResponse).getHeader("X-Request-Id"));
            var request = new MockHttpServletRequest("GET", "/api/books");
            request.addHeader("X-Request-Id", "abc-123");

            filter.doFilter(request, response, chain);

            assertThat(headerDuringChain).hasValue("abc-123");
            assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc-123");
        }
    }
}
