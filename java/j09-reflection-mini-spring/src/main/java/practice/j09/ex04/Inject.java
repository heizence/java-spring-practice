package practice.j09.ex04;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the constructor to create a bean with, when its class has several public constructors. Like
 * {@code jakarta.inject.Inject}, or Spring's {@code @Autowired} on a constructor. Given code: you don't need to
 * change it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface Inject {
}
