package com.example.bookstore.book;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

/**
 * Settings for cover images, {@code bookstore.covers.*} in {@code application.yml}.
 *
 * @param maxSize the largest cover that an upload may have, such as {@code 1MB}. {@link DataSize} reads
 *                {@code 1MB} as 1,048,576 bytes.
 */
@ConfigurationProperties(prefix = "bookstore.covers")
@Validated
public record CoverProperties(@NotNull DataSize maxSize) {
}
