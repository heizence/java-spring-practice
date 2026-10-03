package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;

import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * The shutdown settings, and what the readiness probe says while the application refuses traffic. The tests publish
 * the availability events that Spring Boot publishes during a shutdown, and put the state back afterwards.
 */
@IntegrationTest
@DisplayName("S14-03 · Graceful shutdown and packaging")
class ShutdownSettingsTest {

    @Autowired
    private MockMvcTester mvc;

    @Autowired
    private ApplicationContext context;

    @Autowired
    private Environment environment;

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("states graceful shutdown in application.yml, although it's Spring Boot's default")
        void gracefulShutdownIsExplicit() {
            assertThat(environment.getProperty("server.shutdown")).isEqualTo("graceful");
        }

        @Test
        @DisplayName("gives each shutdown phase at most 20 seconds")
        void phaseTimeout() {
            assertThat(environment.getProperty("spring.lifecycle.timeout-per-shutdown-phase", Duration.class))
                    .isEqualTo(Duration.ofSeconds(20));
        }

        @Test
        @DisplayName("lets running @Async tasks finish at shutdown, for up to 10 seconds")
        void asyncTasksFinish() {
            assertThat(environment.getProperty("spring.task.execution.shutdown.await-termination", Boolean.class))
                    .isTrue();
            assertThat(environment.getProperty("spring.task.execution.shutdown.await-termination-period",
                    Duration.class))
                    .isEqualTo(Duration.ofSeconds(10));
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("while the application refuses traffic, readiness is 503 OUT_OF_SERVICE and liveness stays UP")
        void readinessFollowsTheAvailabilityState() {
            try {
                AvailabilityChangeEvent.publish(context, ReadinessState.REFUSING_TRAFFIC);

                assertThat(mvc.get().uri("/actuator/health/readiness"))
                        .hasStatus(HttpStatus.SERVICE_UNAVAILABLE)
                        .bodyJson().extractingPath("$.status").isEqualTo("OUT_OF_SERVICE");
                assertThat(mvc.get().uri("/actuator/health/liveness"))
                        .hasStatusOk()
                        .bodyJson().extractingPath("$.status").isEqualTo("UP");
            } finally {
                AvailabilityChangeEvent.publish(context, ReadinessState.ACCEPTING_TRAFFIC);
            }
        }
    }
}
