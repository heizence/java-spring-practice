package com.example.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.bookstore.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.availability.AvailabilityChangeEvent;
import org.springframework.boot.availability.ReadinessState;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * What happens when the application stops, with a real web server: this test starts its own copy of the application
 * on a free port, connected to the tests' MySQL container, and closes it the way a stop signal does.
 */
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("S14-03 · Graceful shutdown and packaging")
class GracefulShutdownTest {

    private static final String GRACEFUL_SHUTDOWN_STARTS =
            "Commencing graceful shutdown. Waiting for active requests to complete";

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @Timeout(value = 60, unit = TimeUnit.SECONDS) // starts a whole application with a web server
        @DisplayName("closing the application first refuses traffic, then shuts the web server down gracefully")
        void refusesTrafficThenShutsDownGracefully(CapturedOutput output) {
            ConfigurableApplicationContext application = SpringApplication.from(BookstoreApplication::main)
                    .with(TestcontainersConfiguration.class)
                    .run("--server.port=0")
                    .getApplicationContext();
            List<ReadinessState> readinessChanges = new CopyOnWriteArrayList<>();
            var outputLengthWhenRefusing = new AtomicInteger(-1);
            application.addApplicationListener((ApplicationListener<ApplicationEvent>) event -> {
                if (event instanceof AvailabilityChangeEvent<?> change
                        && change.getState() instanceof ReadinessState state) {
                    readinessChanges.add(state);
                    if (state == ReadinessState.REFUSING_TRAFFIC) {
                        outputLengthWhenRefusing.set(output.getOut().length());
                    }
                }
            });

            application.close();

            assertThat(readinessChanges).containsExactly(ReadinessState.REFUSING_TRAFFIC);
            assertThat(output.getOut())
                    .contains(GRACEFUL_SHUTDOWN_STARTS)
                    .contains("Graceful shutdown complete");
            assertThat(output.getOut().indexOf(GRACEFUL_SHUTDOWN_STARTS))
                    .as("the graceful shutdown starts after readiness changed to REFUSING_TRAFFIC")
                    .isGreaterThan(outputLengthWhenRefusing.get());
        }
    }
}
