package practice.j09.ex02;

/**
 * The annotated {@code String} field must not be {@code null}, empty or only whitespace.
 *
 * <p>Like every constraint annotation in this package, it must be visible to {@link Validator} at runtime, and it
 * belongs on fields only. A record component counts: the compiler copies an annotation that targets fields to the
 * record's private field.
 */
// TODO: J09-02 — add @Retention and @Target, so that the Javadoc above is true.
public @interface NotBlank {
}
