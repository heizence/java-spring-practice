package com.example.bookstore.book;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** The body of {@code PUT /api/books/{id}/price}: the new price in won. */
public record ChangePriceRequest(@NotNull @Positive Long price) {
}
