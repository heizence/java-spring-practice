package practice.j09.ex02;

/**
 * The annotated integer field lies between {@link #min()} and {@link #max()}, both included. {@code null} passes.
 *
 * <p>Visible at runtime, and for fields only (record components included), like {@link NotBlank}.
 *
 * <p>Both elements have defaults, so {@code @Range(min = 0)} means "not negative".
 */
// TODO: J09-02 — add @Retention and @Target, like for @NotBlank.
public @interface Range {

    /** The smallest allowed value. */
    long min() default Long.MIN_VALUE;

    /** The largest allowed value. */
    long max() default Long.MAX_VALUE;
}
