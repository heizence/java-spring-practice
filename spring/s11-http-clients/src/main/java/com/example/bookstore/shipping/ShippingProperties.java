package com.example.bookstore.shipping;

import java.net.URI;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the carrier's API, under {@code bookstore.shipping} in {@code application.yml}.
 *
 * @param baseUrl where the carrier's API lives, such as {@code https://api.carrier.example}
 */
@Validated
@ConfigurationProperties("bookstore.shipping")
public record ShippingProperties(@NotNull URI baseUrl) {
}
