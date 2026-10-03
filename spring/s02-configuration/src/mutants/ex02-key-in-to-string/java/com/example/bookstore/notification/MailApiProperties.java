package com.example.bookstore.notification;

import java.net.URI;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/** Mutant: keeps the record's generated toString(), which prints the API key. */
@ConfigurationProperties("bookstore.mail.api")
@Validated
public record MailApiProperties(@NotNull URI url, @NotBlank String key) {
}
