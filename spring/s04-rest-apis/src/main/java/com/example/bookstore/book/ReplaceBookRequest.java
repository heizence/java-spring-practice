package com.example.bookstore.book;

import java.time.LocalDate;

/**
 * The body of {@code PUT /api/books/{id}}: everything about a book that may change. {@code PUT} replaces the book,
 * so every field is required. The ISBN never changes, so it isn't here; an {@code isbn} property in the JSON is
 * ignored like any other unknown property.
 *
 * <h2>Rules</h2>
 * The same as for {@link CreateBookRequest}, with the same messages:
 * <ul>
 *   <li>{@code title}: required and not blank ({@code must not be blank}); at most 200 characters
 *       ({@code size must be between 0 and 200}).</li>
 *   <li>{@code author}: required and not blank ({@code must not be blank}); at most 100 characters
 *       ({@code size must be between 0 and 100}).</li>
 *   <li>{@code categoryId}: required ({@code must not be null}).</li>
 *   <li>{@code price}: required ({@code must not be null}); more than 0 won ({@code must be greater than 0}).</li>
 *   <li>{@code publishedDate}: required ({@code must not be null}).</li>
 * </ul>
 */
// TODO: S04-07: put the rules above on the components, as Bean Validation constraints.
public record ReplaceBookRequest(
        String title,
        String author,
        Long categoryId,
        Long price,
        LocalDate publishedDate) {
}
