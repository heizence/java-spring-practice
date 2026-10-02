package com.example.bookstore.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Every kind of error becomes the problem-detail JSON that {@code spring/BOOKSTORE.md} documents. A web slice test:
 * it starts only the web layer (no database), which also proves that web slice tests start without JPA.
 */
@WebMvcTest(controllers = GlobalExceptionHandlerTest.OrderTestController.class)
@Import({GlobalExceptionHandlerTest.OrderTestController.class, GlobalExceptionHandlerTest.QuantityChecker.class})
@DisplayName("Template · Problem-detail error responses")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Broken business rules")
    class BusinessRules {

        @Test
        @DisplayName("a BusinessException becomes the status and code of its ErrorCode, with its message as detail")
        void businessException() {
            assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(validOrder()))
                    .hasStatus(HttpStatus.CONFLICT)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Conflict",
                              "status": 409,
                              "detail": "Only 3 copies of book 7 are left, but 5 were requested.",
                              "instance": "/api/orders",
                              "code": "OUT_OF_STOCK"
                            }""");
        }

        @Test
        @DisplayName("a BusinessException without a message uses the ErrorCode's default message")
        void defaultMessage() {
            assertThat(mvc.get().uri("/api/orders/latest"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "The order was not found.",
                              "instance": "/api/orders/latest",
                              "code": "ORDER_NOT_FOUND"
                            }""");
        }
    }

    @Nested
    @DisplayName("Invalid input")
    class InvalidInput {

        @Test
        @DisplayName("an invalid request body lists every invalid field, sorted by field")
        void invalidBody() {
            String order = """
                    { "memberId": null, "lines": [ { "bookId": 7, "quantity": 0 } ] }""";

            assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Invalid request content.",
                              "instance": "/api/orders",
                              "code": "INVALID_INPUT",
                              "errors": [
                                { "field": "lines[0].quantity", "message": "must be greater than or equal to 1" },
                                { "field": "memberId", "message": "must not be null" }
                              ]
                            }""");
        }

        @Test
        @DisplayName("validation messages are in English, even when the client asks for another language")
        void englishMessages() {
            String order = """
                    { "memberId": 1, "lines": [ { "bookId": 7, "quantity": 0 } ] }""";

            assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(order)
                    .header(HttpHeaders.ACCEPT_LANGUAGE, "ko-KR"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .hasPathSatisfying("$.errors[0].message",
                            message -> assertThat(message).asString().isEqualTo("must be greater than or equal to 1"));
        }

        @Test
        @DisplayName("invalid request parameters are listed by parameter name")
        void invalidParameters() {
            assertThat(mvc.get().uri("/api/orders?page=-1&size=500"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Validation failure",
                              "instance": "/api/orders",
                              "code": "INVALID_INPUT",
                              "errors": [
                                { "field": "page", "message": "must be greater than or equal to 0" },
                                { "field": "size", "message": "must be less than or equal to 100" }
                              ]
                            }""");
        }

        @Test
        @DisplayName("a failed @Validated check in a service is invalid input too")
        void constraintViolation() {
            assertThat(mvc.get().uri("/api/orders/check?quantity=0"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Invalid request content.",
                              "instance": "/api/orders/check",
                              "code": "INVALID_INPUT",
                              "errors": [
                                { "field": "check.quantity", "message": "must be greater than or equal to 1" }
                              ]
                            }""");
        }

        @Test
        @DisplayName("malformed JSON is invalid input")
        void malformedJson() {
            String truncatedJson = "{ \"memberId\": ";

            assertThat(mvc.post().uri("/api/orders").contentType(MediaType.APPLICATION_JSON).content(truncatedJson))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Failed to read request",
                              "instance": "/api/orders",
                              "code": "INVALID_INPUT"
                            }""");
        }

        @Test
        @DisplayName("a missing request parameter is invalid input")
        void missingParameter() {
            assertThat(mvc.get().uri("/api/orders?page=0"))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Bad Request",
                              "status": 400,
                              "detail": "Required parameter 'size' is not present.",
                              "instance": "/api/orders",
                              "code": "INVALID_INPUT"
                            }""");
        }
    }

    @Nested
    @DisplayName("Requests that Spring MVC can't route")
    class Routing {

        @Test
        @DisplayName("an unknown path is 404 NOT_FOUND")
        void unknownPath() {
            assertThat(mvc.get().uri("/api/nothing-here"))
                    .hasStatus(HttpStatus.NOT_FOUND)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Not Found",
                              "status": 404,
                              "detail": "No endpoint GET /api/nothing-here.",
                              "instance": "/api/nothing-here",
                              "code": "NOT_FOUND"
                            }""");
        }

        @Test
        @DisplayName("an unsupported HTTP method is 405 METHOD_NOT_ALLOWED")
        void wrongMethod() {
            assertThat(mvc.delete().uri("/api/orders"))
                    .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Method Not Allowed",
                              "status": 405,
                              "detail": "Method 'DELETE' is not supported.",
                              "instance": "/api/orders",
                              "code": "METHOD_NOT_ALLOWED"
                            }""");
        }

        @Test
        @DisplayName("an unsupported request body type is 415 UNSUPPORTED_MEDIA_TYPE")
        void wrongContentType() {
            assertThat(mvc.post().uri("/api/orders").contentType(MediaType.TEXT_PLAIN).content("an order"))
                    .hasStatus(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                    .bodyJson()
                    .hasPathSatisfying("$.code",
                            code -> assertThat(code).asString().isEqualTo("UNSUPPORTED_MEDIA_TYPE"))
                    .hasPathSatisfying("$.status", status -> assertThat(status).asNumber().isEqualTo(415));
        }
    }

    @Nested
    @DisplayName("Bugs")
    class Bugs {

        @Test
        @DisplayName("an unexpected exception is 500 with a generic message that reveals nothing")
        void unexpectedException() {
            assertThat(mvc.get().uri("/api/orders/broken"))
                    .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Internal Server Error",
                              "status": 500,
                              "detail": "An unexpected error occurred.",
                              "instance": "/api/orders/broken",
                              "code": "INTERNAL_ERROR"
                            }""");
        }

        @Test
        @ExtendWith(OutputCaptureExtension.class)
        @DisplayName("an unexpected exception is logged with its stack trace")
        void logsUnexpectedException(CapturedOutput output) {
            mvc.get().uri("/api/orders/broken").exchange();

            assertThat(output.getOut())
                    .contains("ERROR")
                    .contains("Unexpected error in GET /api/orders/broken")
                    .contains("java.lang.IllegalStateException: connection to 10.0.0.12 refused")
                    .contains("at " + OrderTestController.class.getName());
        }
    }

    private static String validOrder() {
        return """
                { "memberId": 1, "lines": [ { "bookId": 7, "quantity": 5 } ] }""";
    }

    /** The request body of the test endpoint, with typical constraints. */
    record PlaceOrderRequest(@NotNull Long memberId, @NotEmpty List<@Valid Line> lines) {

        record Line(@NotNull Long bookId, @Min(1) int quantity) {
        }
    }

    /** Endpoints that fail in every way the handler must cover. */
    @RestController
    static class OrderTestController {

        private final QuantityChecker quantityChecker;

        OrderTestController(QuantityChecker quantityChecker) {
            this.quantityChecker = quantityChecker;
        }

        @PostMapping("/api/orders")
        void placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
            throw new BusinessException(ErrorCode.OUT_OF_STOCK,
                    "Only 3 copies of book 7 are left, but 5 were requested.");
        }

        @GetMapping("/api/orders")
        List<String> listOrders(@RequestParam @Min(0) int page, @RequestParam @Min(1) @Max(100) int size) {
            return List.of();
        }

        @GetMapping("/api/orders/latest")
        void latestOrder() {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        @GetMapping("/api/orders/check")
        void check(@RequestParam int quantity) {
            quantityChecker.check(quantity);
        }

        @GetMapping("/api/orders/broken")
        void broken() {
            throw new IllegalStateException("connection to 10.0.0.12 refused");
        }
    }

    /** A service whose arguments Spring validates ({@code @Validated}), like a module's services may be. */
    @Service
    @Validated
    static class QuantityChecker {

        void check(@Min(1) int quantity) {
        }
    }
}
