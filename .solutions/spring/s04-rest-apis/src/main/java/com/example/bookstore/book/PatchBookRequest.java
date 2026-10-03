package com.example.bookstore.book;

import java.time.LocalDate;

import com.example.bookstore.common.validation.NullOrNotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code PATCH /api/books/{id}}: the fields to change. A field that the JSON doesn't contain is
 * {@code null} here and stays as it is. Every field of a book is required, so {@code null} can't mean "remove the
 * value"; a JSON {@code null} is treated like a missing field.
 *
 * <h2>Rules</h2>
 * The rules of {@link ReplaceBookRequest}, but only for the fields that are present, with the same messages:
 * <ul>
 *   <li>{@code title}: if present, not blank ({@code must not be blank}) and at most 200 characters
 *       ({@code size must be between 0 and 200}).</li>
 *   <li>{@code author}: if present, not blank ({@code must not be blank}) and at most 100 characters
 *       ({@code size must be between 0 and 100}).</li>
 *   <li>{@code price}: if present, more than 0 won ({@code must be greater than 0}).</li>
 *   <li>{@code categoryId} and {@code publishedDate}: any value.</li>
 * </ul>
 */
public record PatchBookRequest(
        @NullOrNotBlank @Size(max = 200) String title,
        @NullOrNotBlank @Size(max = 100) String author,
        Long categoryId,
        @Positive Long price,
        LocalDate publishedDate) {
}
