package com.example.bookstore.common.web;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the admin API, {@code bookstore.admin.*} in {@code application.yml}.
 *
 * <p>{@code @Validated} checks the values when the application starts: a missing token stops the start instead of
 * leaving the admin API open.
 *
 * @param token the secret that requests to {@code /api/admin/**} must send in the {@code X-Admin-Token} header. Set
 *              it with the environment variable {@code BOOKSTORE_ADMIN_TOKEN} outside your own computer, and never
 *              commit a real one.
 */
@ConfigurationProperties(prefix = "bookstore.admin")
@Validated
public record AdminProperties(@NotBlank String token) {
}
