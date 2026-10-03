package com.example.bookstore.common.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * The string is either {@code null} or contains at least one character that isn't whitespace.
 *
 * <p>For optional fields, such as those of a {@code PATCH} request: {@code null} means "not sent, leave the value as
 * it is", but a value that is sent must not be blank. {@code @NotBlank} doesn't fit, because it also rejects
 * {@code null}. Most other constraints ({@code @Size}, {@code @Positive}, {@code @Pattern}) already accept
 * {@code null}, so they need no such variant.
 *
 * <pre>{@code
 * public record PatchBookRequest(@NullOrNotBlank @Size(max = 200) String title, ...) { }
 * }</pre>
 *
 * <p>A custom constraint is an annotation marked with {@link Constraint @Constraint} that names its validator class,
 * plus the three attributes that every constraint must have: {@code message}, {@code groups} and {@code payload}.
 */
@Documented
@Constraint(validatedBy = NullOrNotBlankValidator.class)
@Target({ElementType.METHOD, ElementType.FIELD, ElementType.ANNOTATION_TYPE, ElementType.CONSTRUCTOR,
        ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NullOrNotBlank {

    /** The same message as {@code @NotBlank}, so that clients see one wording for one rule. */
    String message() default "must not be blank";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
