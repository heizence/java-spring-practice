package com.example.bookstore.common.audit;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.example.bookstore.common.error.BusinessException;

/**
 * One line of the {@link AuditLog}: which operation ran, with which arguments, when, and whether it worked.
 *
 * <pre>
 * 2026-10-15T00:00:00Z  CouponService.issue(AUTUMN3000, 12)  SUCCEEDED
 * 2026-10-15T00:00:00Z  CouponService.issue(AUTUMN3000, 1)   FAILED COUPON_ALREADY_ISSUED
 * </pre>
 *
 * @param at        when the call ended, from the application's {@code Clock}
 * @param action    the simple name of the class that declares the method, a dot and the method's name, such as
 *                  {@code CouponService.issue}
 * @param arguments the call's arguments, separated by ", ", such as {@code AUTUMN3000, 12}
 * @param outcome   whether the call returned normally or threw
 * @param error     {@code null} for a success. For a failure: the {@link BusinessException#getErrorCode() error
 *                  code} of a {@link BusinessException} (such as {@code COUPON_SOLD_OUT}), or the simple class name
 *                  of any other exception (such as {@code IllegalArgumentException})
 */
public record AuditEntry(Instant at, String action, String arguments, Outcome outcome, String error) {

    /** How an audited call ended. */
    public enum Outcome {
        SUCCEEDED,
        FAILED
    }

    public AuditEntry {
        Objects.requireNonNull(at, "at");
        Objects.requireNonNull(action, "action");
        Objects.requireNonNull(arguments, "arguments");
        Objects.requireNonNull(outcome, "outcome");
        if ((outcome == Outcome.FAILED) != (error != null)) {
            throw new IllegalArgumentException("A FAILED entry needs an error, a SUCCEEDED entry has none: "
                    + outcome + ", " + error);
        }
    }

    /** An entry for a call that returned normally. */
    public static AuditEntry succeeded(Instant at, String action, Object[] arguments) {
        return new AuditEntry(at, action, describe(arguments), Outcome.SUCCEEDED, null);
    }

    /** An entry for a call that threw {@code failure}. */
    public static AuditEntry failed(Instant at, String action, Object[] arguments, Throwable failure) {
        Objects.requireNonNull(failure, "failure");
        String error = failure instanceof BusinessException business
                ? business.getErrorCode().name()
                : failure.getClass().getSimpleName();
        return new AuditEntry(at, action, describe(arguments), Outcome.FAILED, error);
    }

    private static String describe(Object[] arguments) {
        return Arrays.stream(arguments).map(String::valueOf).collect(Collectors.joining(", "));
    }

    /** One line for the log file, as in the class description. */
    @Override
    public String toString() {
        return "%s  %s(%s)  %s%s".formatted(at, action, arguments, outcome, error == null ? "" : " " + error);
    }
}
