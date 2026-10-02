package practice.j09.ex02;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The annotated {@code String} field has at most {@link #value()} characters. {@code null} passes.
 *
 * <p>Visible at runtime, and for fields only (record components included), like {@link NotBlank}.
 *
 * <p>The single element is named {@code value}, so you can write {@code @MaxLength(20)} instead of
 * {@code @MaxLength(value = 20)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface MaxLength {

    /** The maximum number of characters. */
    int value();
}
