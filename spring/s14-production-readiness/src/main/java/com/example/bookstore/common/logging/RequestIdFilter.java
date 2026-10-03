package com.example.bookstore.common.logging;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id that appears in all of its log lines, so that you can find every line of one request
 * among the lines of all the others.
 *
 * <ul>
 *   <li>The id is the request's {@value #HEADER} header if it has one with 1 to 64 characters, each of them a letter
 *       (A–Z, a–z), a digit, {@code -}, {@code _} or {@code .}. A proxy or another service may have set it, and
 *       using the same id lets you follow the request across services. Any other value, or no header at all, gets
 *       a new random UUID instead: a header could contain line breaks that forge log lines, or be huge.</li>
 *   <li>The id is in the MDC under {@value #MDC_KEY} while the request runs, so that the log pattern can print it.</li>
 *   <li>The response has the id in its {@value #HEADER} header, so that a client can quote it in a bug report.</li>
 *   <li>The id leaves the MDC when the request is done, also when the request failed. The thread serves other
 *       requests next, and they must not show this id.</li>
 * </ul>
 *
 * <p>It runs before all other filters, so that their log lines have the id too.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";
    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // TODO: S14-01 — this placeholder only passes the request on. A filter that throws would break every
        // request of the application, not only this ticket's tests.
        chain.doFilter(request, response);
    }

    /**
     * The id for a request with this {@value #HEADER} header: the header itself if it is safe to log, as described
     * above, and otherwise a new random UUID.
     *
     * @param header the header's value, or {@code null} if the request has none
     */
    static String requestIdFrom(String header) {
        // TODO: S14-01
        throw new UnsupportedOperationException("TODO: S14-01");
    }
}
