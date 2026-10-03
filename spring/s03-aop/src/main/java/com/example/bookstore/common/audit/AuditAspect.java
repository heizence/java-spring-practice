package com.example.bookstore.common.audit;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * Writes an {@link AuditEntry} to the {@link AuditLog} for every call to an audited operation.
 *
 * <h2>Rules</h2>
 * <ul>
 *   <li>Audited operations are the {@code public} methods of classes annotated with
 *       {@link org.springframework.stereotype.Service @Service} in the package {@code com.example.bookstore.coupon},
 *       except methods whose names start with {@code find}: by the bookstore's naming convention, they only read.</li>
 *   <li>Nothing else is audited: not services in other packages, not other beans in the coupon package (such as
 *       {@code @Repository} or {@code @Component} classes), not {@code find…} methods, and not methods that aren't
 *       {@code public}. (Spring's class proxies can intercept {@code protected} and package-private methods too.)</li>
 *   <li>A call that returns normally adds {@link AuditEntry#succeeded}; a call that throws an exception adds
 *       {@link AuditEntry#failed}. Exactly one entry per call.</li>
 *   <li>The action is the simple name of the class that declares the method, a dot and the method's name
 *       ({@code CouponService.issue}); the arguments are the call's arguments; the time is
 *       {@code clock.instant()}.</li>
 *   <li>Auditing never changes the outcome: the caller gets the method's result, or its exception.</li>
 * </ul>
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLog auditLog;
    private final Clock clock;

    // TODO: S03-02 — declare a pointcut for the audited operations (named pointcuts can be combined with &&, ||, !)

    /** Adds a {@code SUCCEEDED} entry for a call to an audited operation that returned normally. */
    // TODO: S03-02 — the advice annotation
    public void recordSuccess(JoinPoint call) {
        // TODO: S03-02
        throw new UnsupportedOperationException("TODO: S03-02");
    }

    /**
     * Adds a {@code FAILED} entry for a call to an audited operation that threw {@code failure}. The exception then
     * continues to the caller.
     */
    // TODO: S03-02 — the advice annotation, which also passes the exception to the failure parameter
    public void recordFailure(JoinPoint call, Exception failure) {
        // TODO: S03-02
        throw new UnsupportedOperationException("TODO: S03-02");
    }
}
