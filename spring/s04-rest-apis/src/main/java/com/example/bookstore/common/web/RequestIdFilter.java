package com.example.bookstore.common.web;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request an id and sends it back in the {@value #HEADER} response header, so that a client can quote
 * it in a bug report and we can find the request in our logs (S14 puts it on every log line).
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>If the request has a valid {@value #HEADER} header, for example from a proxy or another service in front of
 *       us, the filter keeps that id. Valid means 1 to 64 characters, each a letter, a digit, {@code -}, {@code _} or
 *       {@code .}: enough for UUIDs and the ids of common proxies.</li>
 *   <li>Otherwise it creates a new random UUID. An invalid id is replaced, never echoed: it could be a huge string or
 *       contain line breaks that forge log lines.</li>
 *   <li>The response header is set <em>before</em> the rest of the chain runs. Once the controller has written the
 *       body, the response may already be on its way to the client, and later headers would be lost.</li>
 *   <li>Every request passes through: the filter never rejects one.</li>
 * </ul>
 *
 * <p>A filter is a servlet component, not a Spring MVC one: it runs for every request, before Spring MVC chooses a
 * controller, also for requests that end in a 404. It isn't a {@code @Component}: {@code WebConfiguration} registers
 * it, which lets us choose its position among the filters.
 *
 * <p>TODO: S04-05: register it in {@code WebConfiguration}, at the front of the filter chain.
 */
public class RequestIdFilter extends OncePerRequestFilter {

    /** The header that carries the request id, in the request and in the response. */
    public static final String HEADER = "X-Request-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // TODO: S04-05
        throw new UnsupportedOperationException("TODO: S04-05");
    }
}
