package practice.j09.ex04;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class whose objects the {@link MiniContainer} creates and wires: a <em>component</em>. The objects it
 * creates are called <em>beans</em>. Like Spring's {@code @Component}. Given code: you don't need to change it.
 *
 * <p>{@code TYPE} includes annotation types, so this annotation can also mark other annotations, such as
 * {@link Service}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Component {
}
