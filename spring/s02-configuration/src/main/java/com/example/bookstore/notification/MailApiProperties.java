package com.example.bookstore.notification;

import java.net.URI;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * How the bookstore reaches its mail provider's HTTP API, from the configuration properties under
 * {@code bookstore.mail.api}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>A {@code @ConfigurationProperties} record with the prefix {@code bookstore.mail.api}, validated at startup
 *       ({@code @Validated}): {@code url} must be set, and {@code key} must not be blank.</li>
 *   <li>The URL is the same on every production server, so it lives in {@code application-prod.yml}:
 *       {@code https://mail-api.example.com/v1/messages}.</li>
 *   <li>The key is a secret, so it never appears in a file in git. Each server gets it from the environment variable
 *       {@code BOOKSTORE_MAIL_API_KEY}, which relaxed binding turns into {@code bookstore.mail.api.key}.</li>
 *   <li>{@link #toString()} shows the URL but hides the key, so that the key can't leak into a log:
 *       {@code MailApiProperties[url=https://mail-api.example.com/v1/messages, key=******]}. A record's generated
 *       {@code toString()} would print every component.</li>
 *   <li>Only the {@link ApiMailSender} needs these settings, so {@link MailConfiguration} registers them only outside
 *       the {@code local} profile.</li>
 * </ul>
 *
 * @param url the provider's endpoint for sending mail
 * @param key the secret API key
 */
@ConfigurationProperties("bookstore.mail.api")
@Validated
public record MailApiProperties(@NotNull URI url, @NotBlank String key) {

    // Configuration objects get logged: in debug output, in error messages, by Actuator. Six stars say "set, but
    // secret" without telling anyone the key's length.
    @Override
    public String toString() {
        return "MailApiProperties[url=%s, key=******]".formatted(url);
    }
}
