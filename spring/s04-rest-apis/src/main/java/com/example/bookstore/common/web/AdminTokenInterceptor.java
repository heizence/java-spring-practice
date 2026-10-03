package com.example.bookstore.common.web;

import java.security.MessageDigest;

import com.example.bookstore.common.error.BusinessException;
import com.example.bookstore.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Lets a request through only if it sends the admin token ({@link AdminProperties#token()}) in the {@value #HEADER}
 * header. Registered for {@code /api/admin/**} only: see {@code WebConfiguration}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The header must be exactly the configured token: same letters, same case, nothing more or less.</li>
 *   <li>Otherwise, including when the header is missing, {@link #preHandle} throws a {@link BusinessException} with
 *       {@link ErrorCode#UNAUTHORIZED} and the message {@code Send the admin token in the X-Admin-Token header.}, and
 *       the controller never runs. An exception from an interceptor reaches {@code GlobalExceptionHandler}, so the
 *       client gets the usual problem detail.</li>
 *   <li>The comparison takes the same time however many characters match ({@link MessageDigest#isEqual}).
 *       {@code String.equals} returns at the first wrong character, and an attacker who measures response times could
 *       guess the token one character at a time.</li>
 * </ul>
 *
 * <p>TODO: S04-05: register it in {@code WebConfiguration}, for {@code /api/admin/**} only.
 */
@Component
@RequiredArgsConstructor
public class AdminTokenInterceptor implements HandlerInterceptor {

    /** The request header that carries the admin token. */
    public static final String HEADER = "X-Admin-Token";

    private final AdminProperties adminProperties;

    /** Runs before the controller; returning {@code true} lets the request continue. */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // TODO: S04-05
        throw new UnsupportedOperationException("TODO: S04-05");
    }
}
