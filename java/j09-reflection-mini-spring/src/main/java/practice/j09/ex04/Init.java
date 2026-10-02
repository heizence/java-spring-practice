package practice.j09.ex04;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a public method without parameters that the container calls once, right after it has created the bean.
 * Like {@code jakarta.annotation.PostConstruct}. Given code: you don't need to change it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Init {
}
