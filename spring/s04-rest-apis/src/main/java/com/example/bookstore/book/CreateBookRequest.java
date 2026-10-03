package com.example.bookstore.book;

import java.time.LocalDate;

/**
 * The body of {@code POST /api/books}: a new book for the catalog.
 *
 * <h2>Rules</h2>
 * Each rule ends with the exact message that a broken rule reports.
 * <ul>
 *   <li>{@code isbn}: required ({@code must not be null}); exactly 13 digits, without hyphens
 *       ({@code must be 13 digits}).</li>
 *   <li>{@code title}: required and not blank ({@code must not be blank}); at most 200 characters
 *       ({@code size must be between 0 and 200}).</li>
 *   <li>{@code author}: required and not blank ({@code must not be blank}); at most 100 characters
 *       ({@code size must be between 0 and 100}).</li>
 *   <li>{@code categoryId}: required ({@code must not be null}).</li>
 *   <li>{@code price}: required ({@code must not be null}); more than 0 won ({@code must be greater than 0}).</li>
 *   <li>{@code publishedDate}: required ({@code must not be null}).</li>
 * </ul>
 * A value that breaks several rules reports each of them: an absent title reports {@code must not be blank} once,
 * because a missing value is also blank.
 *
 * <p>The numbers are {@code Long}, not {@code long}: a missing {@code price} then becomes {@code null}, which
 * validation reports by name. Jackson 3 can't put a missing value into a {@code long} and would reject the whole body
 * with a vague "Failed to read request".
 */
// TODO: S04-02: put the rules above on the components, as Bean Validation constraints.
public record CreateBookRequest(
        String isbn,
        String title,
        String author,
        Long categoryId,
        Long price,
        LocalDate publishedDate) {
}
