package com.example.bookstore.common.error;

/**
 * One invalid value in a request, as listed in the {@code errors} property of a {@code 400 Bad Request} response.
 *
 * @param field   where the value is: a JSON property path such as {@code lines[0].quantity}, or the name of a request
 *                parameter or path variable such as {@code size}
 * @param message what is wrong with it, such as {@code must be greater than or equal to 1}
 */
public record FieldViolation(String field, String message) {
}
