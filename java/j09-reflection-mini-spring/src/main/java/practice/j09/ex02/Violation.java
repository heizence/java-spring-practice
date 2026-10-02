package practice.j09.ex02;

import java.util.Objects;

/**
 * One broken rule: which field, and what's wrong with it. Given code: you don't need to change it.
 *
 * @param field   the field's name, such as {@code "username"}
 * @param message what's wrong, such as {@code "must not be blank"}
 */
public record Violation(String field, String message) {

    public Violation {
        Objects.requireNonNull(field, "field");
        Objects.requireNonNull(message, "message");
    }
}
