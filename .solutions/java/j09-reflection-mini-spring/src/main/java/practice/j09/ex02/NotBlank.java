package practice.j09.ex02;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated {@code String} field must not be {@code null}, empty or only whitespace.
 *
 * <p>Like every constraint annotation in this package, it must be visible to {@link Validator} at runtime, and it
 * belongs on fields only. A record component counts: the compiler copies an annotation that targets fields to the
 * record's private field.
 */
// RUNTIME: without it, the default (CLASS) keeps the annotation in the .class file, but reflection can't see it.
@Retention(RetentionPolicy.RUNTIME)
// FIELD: on a record component, the compiler then puts the annotation on the private field only (not on the
// accessor or the constructor parameter), which is exactly where the validator looks.
@Target(ElementType.FIELD)
public @interface NotBlank {
}
