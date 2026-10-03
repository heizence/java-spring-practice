package com.example.bookstore.warehouse;

import java.net.URI;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Settings of the central warehouse's API, under {@code bookstore.warehouse} in {@code application.yml}.
 *
 * @param baseUrl where the warehouse's API lives, such as {@code https://warehouse.bookstore.example}
 */
@Validated
@ConfigurationProperties("bookstore.warehouse")
public record WarehouseProperties(@NotNull URI baseUrl) {
}
