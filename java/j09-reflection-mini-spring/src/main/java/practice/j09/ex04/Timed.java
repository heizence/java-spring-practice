package practice.j09.ex04;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional step 5: marks a method whose calls the container measures, like Micrometer's {@code @Timed}. Given code:
 * you don't need to change it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Timed {
}
