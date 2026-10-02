package practice.j09.ex03;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method that must run in a transaction: a tiny version of Spring's {@code @Transactional}. It does nothing
 * by itself; {@link Proxies#transactional} looks for it. Given code: you don't need to change it.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Transactional {
}
