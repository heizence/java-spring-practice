package com.example.bookstore.common.audit;

import java.time.Clock;

import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
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

    // Small named pointcuts, combined below. Each one reads like a sentence, and each can be reused.

    /** Code in the coupon package, and in any package below it that the feature may grow later. */
    @Pointcut("within(com.example.bookstore.coupon..*)")
    void inCouponPackage() {
    }

    /** Code in a class annotated with @Service (not @Repository, @Component or any other stereotype). */
    @Pointcut("@within(org.springframework.stereotype.Service)")
    void inService() {
    }

    /** The execution of any public method. Without it, CGLIB proxies would also audit package-private helpers. */
    @Pointcut("execution(public * *(..))")
    void publicMethod() {
    }

    /** By convention, find… methods only read, so they aren't worth an audit entry. */
    @Pointcut("execution(* find*(..))")
    void readOnlyMethod() {
    }

    @Pointcut("inCouponPackage() && inService() && publicMethod()")
    void auditedOperation() {
    }

    /** Adds a {@code SUCCEEDED} entry for a call to an audited operation that returned normally. */
    // @AfterReturning runs only when the method returned normally, never after an exception.
    @AfterReturning("auditedOperation()")
    public void recordSuccess(JoinPoint call) {
        auditLog.add(AuditEntry.succeeded(clock.instant(), actionOf(call), call.getArgs()));
    }

    /**
     * Adds a {@code FAILED} entry for a call to an audited operation that threw {@code failure}. The exception then
     * continues to the caller.
     */
    // @AfterThrowing runs only when the method threw. throwing = "failure" binds the exception to the parameter of
    // that name, and the parameter's type narrows the match: Exception audits every exception, while a
    // BusinessException parameter would silently skip all the others. The advice can't swallow the exception.
    @AfterThrowing(pointcut = "auditedOperation()", throwing = "failure")
    public void recordFailure(JoinPoint call, Exception failure) {
        auditLog.add(AuditEntry.failed(clock.instant(), actionOf(call), call.getArgs(), failure));
    }

    private static String actionOf(JoinPoint call) {
        Signature signature = call.getSignature();
        return signature.getDeclaringType().getSimpleName() + "." + signature.getName();
    }
}
