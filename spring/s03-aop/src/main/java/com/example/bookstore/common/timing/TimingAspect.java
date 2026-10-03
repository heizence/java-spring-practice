package com.example.bookstore.common.timing;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Measures every call to a method annotated with {@link Timed} and records it in {@link MethodTimings}.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Every call to a method annotated with {@link Timed}, on any bean, is measured. Methods without the annotation
 *       are not.</li>
 *   <li>The duration is the difference between two readings of the {@link Ticker}: one right before the method runs,
 *       one right after it returns or throws.</li>
 *   <li>The name is {@link Timed#value()} if it isn't blank. Otherwise it's the simple name of the class that
 *       declares the method, a dot and the method's name: {@code BookService.search}.</li>
 *   <li>Each call adds one {@link MethodTiming} to {@link MethodTimings}, also when the method throws. Then
 *       {@code failed} is {@code true}.</li>
 *   <li>The caller can't tell that the aspect is there: it gets the method's result unchanged, and when the method
 *       throws, it gets the very same exception object. Checked exceptions are not wrapped.</li>
 * </ul>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class TimingAspect {

    private final Ticker ticker;
    private final MethodTimings timings;

    /**
     * Measures one call of a method annotated with {@link Timed}: the advice for every such method, on any bean.
     *
     * @param call  the intercepted call; {@code call.proceed()} runs the method
     * @param timed the method's {@link Timed} annotation
     * @return what the method returned
     * @throws Throwable whatever the method threw, unchanged
     */
    // TODO: S03-01 — annotate this method so that Spring runs it around every method annotated with @Timed
    public Object time(ProceedingJoinPoint call, Timed timed) throws Throwable {
        // TODO: S03-01
        throw new UnsupportedOperationException("TODO: S03-01");
    }
}
