package com.example.bookstore.book;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * The body of {@code POST /api/categories/{categoryId}/price-changes}: {@code { "percent": 10 }}.
 *
 * @param percent how much every price changes, from -50 (half price) to 50; a negative value lowers the prices
 */
public record PriceChangeRequest(@NotNull @Min(-50) @Max(50) Integer percent) {
}
