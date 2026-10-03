package com.example.bookstore.common.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/**
 * Every error the API can report. The name is the machine-readable {@code code} property of the problem-detail
 * response; clients compare it instead of parsing the human-readable {@code detail}.
 *
 * <p>Throw a {@link BusinessException} with the matching code when a business rule is broken. The first group of
 * codes is generic: {@link GlobalExceptionHandler} uses them for errors that Spring MVC detects before any of our
 * code runs, such as malformed JSON or an unknown path. Add a code here when a module needs a new kind of error,
 * and never rename one: clients depend on the names.
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // Generic errors
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "The request is not valid."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "The resource was not found."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "This HTTP method is not supported for this path."),
    NOT_ACCEPTABLE(HttpStatus.NOT_ACCEPTABLE, "The requested response format is not supported."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "The request body format is not supported."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred."),

    // Security: who you are (401) and what you may do (403)
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A valid access token is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "You are not allowed to do this."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "The email or password is wrong."),

    // A resource that doesn't exist
    BOOK_NOT_FOUND(HttpStatus.NOT_FOUND, "The book was not found."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "The member was not found."),
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "The order was not found."),
    COUPON_NOT_FOUND(HttpStatus.NOT_FOUND, "The coupon was not found."),

    // A broken business rule
    OUT_OF_STOCK(HttpStatus.CONFLICT, "Not enough copies are in stock."),
    INSUFFICIENT_BALANCE(HttpStatus.CONFLICT, "The wallet balance is too low."),
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "No coupons are left."),
    COUPON_ALREADY_ISSUED(HttpStatus.CONFLICT, "The member already has this coupon."),
    COUPON_NOT_YET_VALID(HttpStatus.CONFLICT, "The coupon is not valid yet."),
    COUPON_EXPIRED(HttpStatus.CONFLICT, "The coupon has expired."),
    COUPON_ALREADY_USED(HttpStatus.CONFLICT, "The coupon has already been used."),
    INVALID_ORDER_STATUS(HttpStatus.CONFLICT, "The order's status doesn't allow this change."),
    INVALID_PAYMENT_STATUS(HttpStatus.CONFLICT, "The payment's status doesn't allow this change."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "The email address is already registered.");

    /** The HTTP status of the response. */
    private final HttpStatus status;

    /** The {@code detail} of the response when the exception doesn't give a more specific one. */
    private final String defaultMessage;
}
