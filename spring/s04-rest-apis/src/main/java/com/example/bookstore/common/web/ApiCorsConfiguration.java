package com.example.bookstore.common.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Lets the shop's web front end call the API from the browser: CORS (cross-origin resource sharing).
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>For every path under {@code /api/}.</li>
 *   <li>Only from the origins in {@link CorsProperties#allowedOrigins()}; the browser blocks every other web page.</li>
 *   <li>The methods {@code GET}, {@code POST}, {@code PUT}, {@code PATCH} and {@code DELETE}.</li>
 *   <li>The request headers {@code Content-Type}, {@code If-Match}, {@code If-None-Match}, {@code API-Version},
 *       {@code X-Member-Id} and {@code X-Request-Id}. Not {@code X-Admin-Token}: the shop isn't an admin tool.</li>
 *   <li>The page's JavaScript may read the response headers {@code ETag}, {@code Location} and
 *       {@code X-Request-Id}. Browsers hide all but a few basic response headers unless the server exposes them.</li>
 *   <li>Browsers may cache the answer to a preflight request for one hour.</li>
 *   <li>No credentials (cookies): the API doesn't use them.</li>
 * </ul>
 */
@Configuration(proxyBeanMethods = false)
public class ApiCorsConfiguration implements WebMvcConfigurer {

    /**
     * S04-10: Spring calls this method while the application starts, so it must never throw: a failure here would
     * stop the whole application. That's why it is empty instead of throwing like the other stubs.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // TODO: S04-10
    }
}
