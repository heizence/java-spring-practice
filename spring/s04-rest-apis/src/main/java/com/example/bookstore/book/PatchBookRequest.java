package com.example.bookstore.book;

import java.time.LocalDate;

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
// TODO: S04-07: put the rules above on the components, as Bean Validation constraints. For "not blank, if present",
// see com.example.bookstore.common.validation.NullOrNotBlank.
public record PatchBookRequest(
        String title,
        String author,
        Long categoryId,
        Long price,
        LocalDate publishedDate) {
}
