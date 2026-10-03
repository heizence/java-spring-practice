package com.example.bookstore.common.timing;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Measures how long each call to the annotated method takes. {@link TimingAspect} records every call in
 * {@link MethodTimings}.
 *
 * <pre>{@code
 * @Timed("book.search")
 * public List<Book> search(String keyword) { ... }
 * }</pre>
 *
 * <p>The annotation is plain data: nothing happens until the aspect looks for it. It must be kept until runtime
 * ({@link RetentionPolicy#RUNTIME}), or the aspect couldn't see it. Like every Spring AOP feature, it only works on
 * calls that reach a Spring bean through its proxy: not on calls from inside the same bean, and not on
 * {@code private} or {@code final} methods.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Timed {

    /**
     * The name to record the timing under, such as {@code book.search}. When it's blank (the default is empty), the
     * timing is recorded as {@code ClassName.methodName}, such as {@code BookService.search}.
     */
    String value() default "";
}
