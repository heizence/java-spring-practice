package com.example.bookstore.common.web;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Which web front ends may call the API from a browser, {@code bookstore.cors.*} in {@code application.yml}.
 *
 * @param allowedOrigins the exact origins (scheme, host and port), such as {@code https://shop.example.com}
 */
@ConfigurationProperties(prefix = "bookstore.cors")
@Validated
public record CorsProperties(@NotEmpty List<String> allowedOrigins) {

    public CorsProperties {
        allowedOrigins = List.copyOf(allowedOrigins);
    }
}
