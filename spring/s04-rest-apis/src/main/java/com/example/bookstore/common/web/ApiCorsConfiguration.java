package com.example.bookstore.common.web;

import java.time.Duration;

import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class ApiCorsConfiguration implements WebMvcConfigurer {

    private final CorsProperties corsProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(corsProperties.allowedOrigins().toArray(String[]::new))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE")
                .allowedHeaders("Content-Type", "If-Match", "If-None-Match", "API-Version", "X-Member-Id",
                        "X-Request-Id")
                .exposedHeaders("ETag", "Location", "X-Request-Id")
                .maxAge(Duration.ofHours(1).toSeconds());
    }
}
