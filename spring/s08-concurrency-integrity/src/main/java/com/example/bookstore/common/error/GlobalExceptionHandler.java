package com.example.bookstore.common.error;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception that leaves a controller into an RFC 9457 problem-detail response with a machine-readable
 * {@code code} (see {@code spring/BOOKSTORE.md}):
 *
 * <pre>{@code
 * { "title": "Conflict", "status": 409, "detail": "Only 3 copies of book 7 are left, but 5 were requested.",
 *   "instance": "/api/orders", "code": "OUT_OF_STOCK" }
 * }</pre>
 *
 * <ul>
 *   <li>{@link BusinessException}: the status and code of its {@link ErrorCode}, its message as {@code detail}.</li>
 *   <li>A transaction that collided with another one ({@link ConcurrencyFailureException}: an optimistic-locking
 *       conflict, a deadlock, a lock wait timeout): {@code 409} with code {@code CONCURRENT_UPDATE}, so the client
 *       knows that it can send the request again.</li>
 *   <li>Invalid input (bean validation, malformed JSON, a parameter of the wrong type, a missing parameter, a sort
 *       by an unknown property): {@code 400} with code {@code INVALID_INPUT}. Validation failures also list each
 *       invalid value in an {@code errors} property of {@link FieldViolation}s.</li>
 *   <li>Other errors that Spring MVC detects (unknown path, wrong HTTP method, unsupported content type): their
 *       usual status, with the generic code for it.</li>
 *   <li>Anything else is a bug: {@code 500} with a generic message, logged with its stack trace. The response never
 *       shows the exception's message, which could reveal internals.</li>
 * </ul>
 *
 * <p>The base class, {@link ResponseEntityExceptionHandler}, already handles Spring MVC's own exceptions; this class
 * adds the {@code code} to all of them in {@link #createResponseEntity}. Responses have no {@code type}: RFC 9457
 * reads a missing {@code type} as {@code about:blank}, meaning that the status says it all.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String CODE = "code";
    private static final String ERRORS = "errors";

    /** Sorted, so that a response with several errors is the same on every run. */
    private static final Comparator<FieldViolation> BY_FIELD_AND_MESSAGE =
            Comparator.comparing(FieldViolation::field).thenComparing(FieldViolation::message);

    /** A broken business rule or a missing resource. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusinessException(BusinessException exception) {
        return problem(exception.getErrorCode(), exception.getMessage(), List.of());
    }

    /**
     * A paged request asked to sort by a property the entity doesn't have ({@code ?sort=colour}). Spring Data notices
     * only when it builds the query, so without this handler the client would get a {@code 500}.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ProblemDetail> handlePropertyReference(PropertyReferenceException exception) {
        return problem(ErrorCode.INVALID_INPUT, "Unknown property '%s'.".formatted(exception.getPropertyName()),
                List.of());
    }

    /**
     * This request's transaction collided with another one and lost: an optimistic-locking conflict
     * ({@code @Version}), a deadlock, or a row lock that wasn't released in time. Sending the same request again can
     * work, so the client gets {@code 409} instead of {@code 500}. Services that expect such collisions retry before
     * it comes to this (S08-02).
     */
    @ExceptionHandler(ConcurrencyFailureException.class)
    public ResponseEntity<ProblemDetail> handleConcurrencyFailure(ConcurrencyFailureException exception) {
        log.warn("A transaction collided with another one: {}", exception.getMessage());
        return problem(ErrorCode.CONCURRENT_UPDATE, ErrorCode.CONCURRENT_UPDATE.getDefaultMessage(), List.of());
    }

    /** Bean validation of a service method's arguments failed ({@code @Validated} on a service). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(ConstraintViolationException exception) {
        List<FieldViolation> errors = exception.getConstraintViolations().stream()
                .map(violation -> new FieldViolation(violation.getPropertyPath().toString(), violation.getMessage()))
                .sorted(BY_FIELD_AND_MESSAGE)
                .toList();
        return problem(ErrorCode.INVALID_INPUT, "Invalid request content.", errors);
    }

    /** Anything not handled elsewhere is a bug. Log it for us; tell the client only that something went wrong. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedException(Exception exception, HttpServletRequest request) {
        log.error("Unexpected error in {} {}", request.getMethod(), request.getRequestURI(), exception);
        return problem(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.getDefaultMessage(), List.of());
    }

    /** A {@code @Valid @RequestBody} (or form object) broke its constraints. */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        List<FieldViolation> errors = new ArrayList<>();
        exception.getBindingResult().getFieldErrors().forEach(error -> errors.add(fieldViolation(error)));
        exception.getBindingResult().getGlobalErrors().forEach(error -> errors.add(objectViolation(error)));
        return handleExceptionInternal(exception, withErrors(exception.getBody(), status, errors),
                headers, status, request);
    }

    /** A constrained request parameter or path variable ({@code @RequestParam @Max(100) int size}) is invalid. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(
            HandlerMethodValidationException exception, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        List<FieldViolation> errors = new ArrayList<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors parameterErrors) {
                // A @Valid object parameter: report the fields inside it, like a @RequestBody.
                parameterErrors.getFieldErrors().forEach(error -> errors.add(fieldViolation(error)));
                parameterErrors.getGlobalErrors().forEach(error -> errors.add(objectViolation(error)));
            } else {
                String parameter = result.getMethodParameter().getParameterName();
                for (MessageSourceResolvable error : result.getResolvableErrors()) {
                    errors.add(new FieldViolation(parameter, messageOf(error)));
                }
            }
        }
        return handleExceptionInternal(exception, withErrors(exception.getBody(), status, errors),
                headers, status, request);
    }

    /** Spring's default detail ("No static resource api/bookz.") would be confusing for an API. */
    @Override
    protected ResponseEntity<Object> handleNoResourceFoundException(
            NoResourceFoundException exception, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        exception.getBody().setDetail("No endpoint %s /%s.".formatted(exception.getHttpMethod(),
                exception.getResourcePath()));
        return handleExceptionInternal(exception, exception.getBody(), headers, status, request);
    }

    /** Every response that the base class builds passes through here: add the {@code code} it lacks. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem
                && (problem.getProperties() == null || !problem.getProperties().containsKey(CODE))) {
            problem.setProperty(CODE, genericCodeFor(statusCode).name());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private static ResponseEntity<ProblemDetail> problem(ErrorCode errorCode, String detail,
                                                         List<FieldViolation> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), detail);
        problem.setProperty(CODE, errorCode.name());
        if (!errors.isEmpty()) {
            problem.setProperty(ERRORS, errors);
        }
        return ResponseEntity.status(errorCode.getStatus()).body(problem);
    }

    /** Adds the code before the errors, so that every response lists its properties in the same order. */
    private static ProblemDetail withErrors(ProblemDetail problem, HttpStatusCode status,
                                            List<FieldViolation> errors) {
        errors.sort(BY_FIELD_AND_MESSAGE);
        problem.setProperty(CODE, genericCodeFor(status).name());
        problem.setProperty(ERRORS, errors);
        return problem;
    }

    private static FieldViolation fieldViolation(FieldError error) {
        return new FieldViolation(error.getField(), messageOf(error));
    }

    /** An error about the object as a whole, such as a class-level constraint: name the object. */
    private static FieldViolation objectViolation(ObjectError error) {
        return new FieldViolation(error.getObjectName(), messageOf(error));
    }

    /** Bean validation always provides a message; a hand-made error might not. */
    private static String messageOf(MessageSourceResolvable error) {
        return Objects.requireNonNullElse(error.getDefaultMessage(), "is invalid");
    }

    /** The code for an error that Spring MVC detected; any other client error counts as invalid input. */
    private static ErrorCode genericCodeFor(HttpStatusCode status) {
        if (status.is5xxServerError()) {
            return ErrorCode.INTERNAL_ERROR;
        }
        HttpStatus knownStatus = HttpStatus.resolve(status.value());
        if (knownStatus == null) {
            return ErrorCode.INVALID_INPUT;
        }
        return switch (knownStatus) {
            case NOT_FOUND -> ErrorCode.NOT_FOUND;
            case METHOD_NOT_ALLOWED -> ErrorCode.METHOD_NOT_ALLOWED;
            case NOT_ACCEPTABLE -> ErrorCode.NOT_ACCEPTABLE;
            case UNSUPPORTED_MEDIA_TYPE -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
            default -> ErrorCode.INVALID_INPUT;
        };
    }
}
