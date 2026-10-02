package practice.j09.ex02;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated integer field lies between {@link #min()} and {@link #max()}, both included. {@code null} passes.
 *
 * <p>Visible at runtime, and for fields only (record components included), like {@link NotBlank}.
 *
 * <p>Both elements have defaults, so {@code @Range(min = 0)} means "not negative".
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Range {

    /** The smallest allowed value. */
    long min() default Long.MIN_VALUE;

    /** The largest allowed value. */
    long max() default Long.MAX_VALUE;
}
