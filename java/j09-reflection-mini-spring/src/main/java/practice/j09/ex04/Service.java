package practice.j09.ex04;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A component that holds business logic. It is annotated with {@link Component} itself (a <em>meta-annotation</em>),
 * so a class marked {@code @Service} is a component too, exactly like Spring's {@code @Service}. Given code: you
 * don't need to change it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface Service {
}
