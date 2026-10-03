package com.example.bookstore.common.error;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A bug: an exception that no handler expects. The application has no endpoint that fails like that on purpose, so
 * this test adds one, {@link BrokenController}. ({@code @Import} gives this class an application context of its
 * own.)
 */
@IntegrationTest
@Import(UnexpectedErrorApiTest.BrokenController.class)
@DisplayName("S04-03 · Error responses with ProblemDetail")
class UnexpectedErrorApiTest {

    @Autowired
    private MockMvcTester mvc;

    @Nested
    @DisplayName("Bugs")
    class Bugs {

        @Test
        @DisplayName("an unexpected exception: 500 with code INTERNAL_ERROR and a message that reveals nothing")
        void unexpectedException() {
            assertThat(mvc.get().uri("/test/broken"))
                    .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                    .hasContentType(MediaType.APPLICATION_PROBLEM_JSON)
                    .bodyJson().isStrictlyEqualTo("""
                            {
                              "title": "Internal Server Error",
                              "status": 500,
                              "detail": "An unexpected error occurred.",
                              "instance": "/test/broken",
                              "code": "INTERNAL_ERROR"
                            }""");
        }

        @Test
        @ExtendWith(OutputCaptureExtension.class)
        @DisplayName("an unexpected exception is logged at ERROR level, with the request and the stack trace")
        void logsUnexpectedException(CapturedOutput output) {
            mvc.get().uri("/test/broken").exchange();

            assertThat(output.getOut())
                    .contains("ERROR")
                    .contains("GET /test/broken")
                    .contains("java.lang.IllegalStateException: connection to 10.0.0.12 refused")
                    .contains("at " + BrokenController.class.getName());
        }
    }

    /** An endpoint with a bug, like a lost database connection: it throws an exception nobody expects. */
    @RestController
    static class BrokenController {

        @GetMapping("/test/broken")
        void broken() {
            throw new IllegalStateException("connection to 10.0.0.12 refused");
        }
    }
}
