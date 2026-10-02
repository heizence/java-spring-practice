package com.example.bookstore.common.error;

import java.util.Objects;

import lombok.Getter;

/**
 * Thrown when a request breaks a business rule or names something that doesn't exist: the client sent a valid
 * request, but it can't be carried out. {@link GlobalExceptionHandler} turns it into a problem-detail response with
 * the {@link ErrorCode}'s status and code, and the exception's message as {@code detail}.
 *
 * <pre>{@code
 * throw new BusinessException(ErrorCode.OUT_OF_STOCK,
 *         "Only %d copies of book %d are left, but %d were requested.".formatted(left, bookId, requested));
 * }</pre>
 *
 * <p>The message is shown to API clients, so write it for them: say what went wrong, with the values involved, and
 * never include internals such as SQL or class names. Programming errors are not business exceptions: throw
 * {@link IllegalArgumentException} or {@link IllegalStateException} for those.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    /** Creates an exception whose message is the error code's default message. */
    public BusinessException(ErrorCode errorCode) {
        this(errorCode, Objects.requireNonNull(errorCode, "errorCode").getDefaultMessage());
    }

    /** Creates an exception with a message that describes this case, such as which book is out of stock. */
    public BusinessException(ErrorCode errorCode, String detail) {
        super(Objects.requireNonNull(detail, "detail"));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }
}
