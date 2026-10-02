package practice.j09.ex02;

import java.util.List;

/**
 * Checks an object's fields against the constraint annotations {@link NotBlank}, {@link MaxLength} and
 * {@link Range}: a tiny version of Jakarta Bean Validation, whose {@code @NotBlank} and {@code @Size} you'll use in
 * Spring.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>The validator looks at the fields declared in the object's own class, of any access level, and skips static
 *       fields. That works for classes and for records alike.</li>
 *   <li>{@code @NotBlank}: the value is not {@code null}, not empty and not only whitespace. Message:
 *       {@code "must not be blank"}.</li>
 *   <li>{@code @MaxLength(n)}: the value has at most {@code n} characters. Message:
 *       {@code "must be at most n characters"}, such as {@code "must be at most 20 characters"}.</li>
 *   <li>{@code @Range(min, max)}: {@code min <= value <= max}. Message: {@code "must be between min and max"}, such as
 *       {@code "must be between 13 and 120"}.</li>
 *   <li>{@code @MaxLength} and {@code @Range} accept {@code null}. Add {@code @NotBlank} to forbid it (Jakarta Bean
 *       Validation works the same way).</li>
 *   <li>A field can carry several annotations. Each broken one gives its own violation.</li>
 *   <li>The result is sorted by field name, then by message. It is unmodifiable, and empty for a valid object.</li>
 *   <li>An annotation on the wrong type of field is a programming mistake: {@link IllegalArgumentException} with
 *       the field's name in the message. {@code @NotBlank} and {@code @MaxLength} belong on {@code String} fields,
 *       {@code @Range} on {@code byte}, {@code short}, {@code int}, {@code long} and their wrapper classes. The
 *       check uses the field's declared type, so it also fails when the value is {@code null}.</li>
 *   <li>A {@code null} object throws {@link NullPointerException}.</li>
 * </ul>
 */
public final class Validator {

    private Validator() {
    }

    /** Returns every rule that {@code object} breaks, sorted by field name, then by message. */
    public static List<Violation> validate(Object object) {
        // TODO: J09-02
        throw new UnsupportedOperationException("TODO: J09-02");
    }
}
